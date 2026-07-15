package com.pharmacore.pharmaciebackend.vente;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import com.pharmacore.pharmaciebackend.client.Client;
import com.pharmacore.pharmaciebackend.client.ClientRepository;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.medicament.Medicament;
import com.pharmacore.pharmaciebackend.medicament.MedicamentRepository;
import com.pharmacore.pharmaciebackend.ordonnance.OrdonnanceRepository;
import com.pharmacore.pharmaciebackend.vente.VenteDtos.CreerVenteRequest;
import com.pharmacore.pharmaciebackend.vente.VenteDtos.LigneRequest;
import com.pharmacore.pharmaciebackend.vente.VenteDtos.VenteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestre la vente réelle — le cœur des briques 5 (Opérations) et 6 (Transactions).
 *
 * <p>Business Core n'accepte qu'un {@code offreId}/{@code quantite} par appel {@code :execute}
 * (backend-test.md §3.2 — vérifié dans {@code ClesContexte.java} côté Business Core) : un panier à
 * plusieurs lignes déclenche donc un appel {@code Vendre:execute} séquentiel par ligne, chacun avec sa
 * propre clé d'idempotence dérivée de l'idempotency key du panier (empêche qu'une même clé, réutilisée
 * pour deux offres différentes, fasse renvoyer par Business Core le résultat déjà tracé de la première
 * ligne pour la seconde). Le statut/transactionId/traceId persistés sur {@code vente} sont ceux de la
 * <b>dernière</b> ligne exécutée — c'est celle qui clôt réellement la vente.
 *
 * <p>Titulaire, Pharmacien Responsable et Caissier peuvent tous vendre (le titulaire est d'abord un
 * pharmacien — cf. AUDIT-PHARMACORE.md). Un médicament sur ordonnance déclenche la
 * règle « ordonnance requise » (effet DEROGER) : le Caissier est bloqué (rôle non autorisé, doit
 * escalader), le Pharmacien Responsable peut vendre en fournissant un motif — c'est Business Core qui
 * vérifie le rôle réel de l'acteur connecté, jamais PharmaCore (le motif transmis sans le bon rôle est
 * rejeté côté plateforme).
 *
 * <p>Aucune ligne n'est persistée si un appel échoue : la vente entière échoue avec l'erreur réelle de
 * Business Core (même logique que la création de médicament — on ne sauvegarde jamais localement tant
 * que la plateforme n'a pas confirmé). Tant que {@code ENGAGER_STOCK} reste bloqué côté Kernel
 * (cf. FEUILLE-DE-ROUTE.md §8), c'est le comportement systématiquement observé : aucune vente ne se
 * termine, et l'erreur 502 remontée est honnête, pas masquée.
 */
@Service
public class VenteService {

    private final VenteRepository repository;
    private final VenteLigneRepository ligneRepository;
    private final MedicamentRepository medicamentRepository;
    private final ClientRepository clientRepository;
    private final OrdonnanceRepository ordonnanceRepository;
    private final BcaasVenteClient bcaasVenteClient;
    private final PharmacoreSession session;
    private final BcaasProperties properties;

    public VenteService(VenteRepository repository, VenteLigneRepository ligneRepository,
                        MedicamentRepository medicamentRepository, ClientRepository clientRepository,
                        OrdonnanceRepository ordonnanceRepository, BcaasVenteClient bcaasVenteClient,
                        PharmacoreSession session, BcaasProperties properties) {
        this.repository = repository;
        this.ligneRepository = ligneRepository;
        this.medicamentRepository = medicamentRepository;
        this.clientRepository = clientRepository;
        this.ordonnanceRepository = ordonnanceRepository;
        this.bcaasVenteClient = bcaasVenteClient;
        this.session = session;
        this.properties = properties;
    }

    @Transactional
    public VenteResponse creer(CreerVenteRequest req) {
        String acteurKernelId = exigerActeurConnecte();

        UUID beneficiaireId = resoudreBeneficiaire(req.clientId());
        if (req.ordonnanceId() != null) {
            ordonnanceRepository.findById(req.ordonnanceId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Ordonnance", req.ordonnanceId()));
        }

        UUID idempotencyKey = UUID.randomUUID();
        BigDecimal montantTotal = BigDecimal.ZERO;
        BcaasVenteClient.ResultatVente dernierResultat = null;
        List<VenteLigne> lignesAPersister = new ArrayList<>();

        for (LigneRequest ligneReq : req.lignes()) {
            Medicament medicament = medicamentRepository.findById(ligneReq.medicamentId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Médicament", ligneReq.medicamentId()));

            Map<String, Object> parametres = construireParametres(
                    medicament, ligneReq.quantite(), beneficiaireId, req.motifDerogation());
            UUID cleLigne = deriverCleLigne(idempotencyKey, medicament.getId());
            dernierResultat = bcaasVenteClient.executerVendre(acteurKernelId, cleLigne, parametres);

            // Vente réellement confirmée par Business Core (aucune exception levée) : le stock local
            // reflète maintenant la vraie vente, au lieu de rester figé indéfiniment (cf. AUDIT-PHARMACORE.md).
            if ("COMPLETEE".equals(dernierResultat.statut())) {
                medicament.decrementerStock(ligneReq.quantite());
                medicamentRepository.save(medicament);
            }

            montantTotal = montantTotal.add(medicament.getPrixUnitaire().multiply(BigDecimal.valueOf(ligneReq.quantite())));
            lignesAPersister.add(new VenteLigne(null, medicament.getId(), ligneReq.quantite(), medicament.getPrixUnitaire()));
        }

        Vente vente = repository.save(new Vente(
                UUID.fromString(properties.businessId()), req.clientId(), req.ordonnanceId(), montantTotal, "XAF",
                req.modePaiement(), dernierResultat.statut(), dernierResultat.transactionKernelId(),
                dernierResultat.traceId(), idempotencyKey));

        List<VenteLigne> lignes = lignesAPersister.stream()
                .map(l -> ligneRepository.save(new VenteLigne(vente.getId(), l.getMedicamentId(), l.getQuantite(),
                        l.getPrixUnitaireFacture())))
                .toList();

        return VenteResponse.depuis(vente, lignes);
    }

    public List<VenteResponse> lister() {
        return repository.findAllByOrderByCreeLeDesc().stream()
                .map(v -> VenteResponse.depuis(v, ligneRepository.findByVenteId(v.getId())))
                .toList();
    }

    public VenteResponse trouver(UUID id) {
        Vente vente = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Vente", id));
        return VenteResponse.depuis(vente, ligneRepository.findByVenteId(id));
    }

    /**
     * Le titulaire est d'abord un pharmacien : il est rattaché à Business Core comme un vrai acteur
     * CAISSIER (cf. FEUILLE-DE-ROUTE.md), donc il peut vendre exactement comme le personnel — Business
     * Core vérifie de toute façon le rôle réel de l'acteur transmis, PharmaCore ne fait que le laisser
     * passer jusque-là (cf. AUDIT-PHARMACORE.md).
     */
    private String exigerActeurConnecte() {
        PharmacoreSession.Role role = session.role();
        if (role == null) {
            throw new BcaasException(401, "Non connecté",
                    "Connectez-vous avant d'encaisser.", null, null, null);
        }
        return role == PharmacoreSession.Role.TITULAIRE
                ? session.titulaireActorIdOuNull()
                : session.acteurKernelIdOuNull();
    }

    private UUID resoudreBeneficiaire(UUID clientId) {
        if (clientId == null) return null;
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Client", clientId));
        return client.getBeneficiaireId();
    }

    /**
     * Construit {@code parametres} exactement selon backend-test.md §3.2 — aucune clé inventée. Pour un
     * médicament sur ordonnance, {@code motif} n'est transmis que s'il a été fourni (Business Core exige
     * un motif ET le rôle PHARMACIEN_RESPONSABLE pour dérober la règle ; sans les deux, la vente est
     * bloquée avec un 422 explicite — vérifié réellement, pas simulé).
     */
    private Map<String, Object> construireParametres(Medicament medicament, int quantite, UUID beneficiaireId,
                                                      String motifDerogation) {
        Map<String, Object> parametres = new HashMap<>();
        parametres.put("offreId", medicament.getOffreId());
        parametres.put("quantite", quantite);
        // beneficiaireId omis (jamais mis à null) : Business Core construit son ContexteEtape via
        // Map.copyOf(parametres), qui lève une NullPointerException dès qu'une valeur est null
        // (contrat Map.ofEntries — vérifié empiriquement, cf. FEUILLE-DE-ROUTE.md). Absence de clé =
        // même sémantique que "pas de bénéficiaire" pour une vente comptant, sans déclencher le bug.
        if (beneficiaireId != null) {
            parametres.put("beneficiaireId", beneficiaireId);
        }
        parametres.put("categorie", medicament.getCategorie());
        if (medicament.isOrdonnanceRequise() && motifDerogation != null && !motifDerogation.isBlank()) {
            parametres.put("motif", motifDerogation);
        }
        return parametres;
    }

    private UUID deriverCleLigne(UUID idempotencyKeyPanier, UUID medicamentId) {
        return UUID.nameUUIDFromBytes((idempotencyKeyPanier + ":" + medicamentId).getBytes(StandardCharsets.UTF_8));
    }
}
