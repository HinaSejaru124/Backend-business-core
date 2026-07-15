package com.pharmacore.pharmaciebackend.admin;

import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.CreerMedicamentRequest;
import com.pharmacore.pharmaciebackend.medicament.MedicamentDtos.MedicamentResponse;
import com.pharmacore.pharmaciebackend.medicament.MedicamentService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Création d'un médicament — espace <b>admin</b> (titulaire, JWT). Déclare l'Offre côté Business Core
 * en design-time ({@link BcaasAdminClient#declarerOffre}) PUIS la fiche locale
 * ({@link MedicamentService#sauvegarderDepuisOffre}), dans cet ordre : l'{@code offreId} réel est requis
 * pour la fiche locale, jamais l'inverse (même principe que l'ancien flux runtime, juste rebranché sur
 * le bon client BCaaS). La lecture du catalogue reste sur {@code GET /api/medicaments} (runtime).
 */
@RestController
@RequestMapping("/api/admin/medicaments")
public class AdminMedicamentController {

    private final BcaasAdminClient adminClient;
    private final MedicamentService medicamentService;
    private final BcaasProperties properties;
    private final PharmacoreSession session;

    public AdminMedicamentController(BcaasAdminClient adminClient, MedicamentService medicamentService,
                                     BcaasProperties properties, PharmacoreSession session) {
        this.adminClient = adminClient;
        this.medicamentService = medicamentService;
        this.properties = properties;
        this.session = session;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentResponse creer(@Valid @RequestBody CreerMedicamentRequest req) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        Map<String, Object> offre = adminClient.declarerOffre(
                properties.typeId(), properties.versionNumber(), req.nom(), req.prixUnitaire());
        UUID offreId = UUID.fromString(String.valueOf(offre.get("id")));
        return medicamentService.sauvegarderDepuisOffre(req, offreId);
    }

    /**
     * Supprime réellement l'Offre côté Business Core puis la fiche locale. Échoue avec l'erreur exacte
     * de Business Core (409 {@code OFFRE_MAPPEE_PRODUIT}) si un produit kernel est déjà mappé sur cette
     * offre — dans ce cas la fiche locale n'est pas touchée (cohérence : jamais de suppression locale
     * sans confirmation réelle côté plateforme, même logique que la création). Si l'Offre est déjà
     * absente côté Business Core (404 — fiche locale orpheline, cf. FEUILLE-DE-ROUTE.md), la suppression
     * locale se poursuit quand même : il n'y a plus rien à confirmer côté plateforme. Si une ordonnance
     * réelle référence encore la fiche (contrainte de clé étrangère {@code ordonnance_ligne}), la
     * suppression est remplacée par un retrait ({@code statut = RETIRE}) — l'historique de prescription
     * réel n'est jamais sacrifié pour un nettoyage de catalogue.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        session.exigerRole(PharmacoreSession.Role.TITULAIRE);
        UUID offreId = medicamentService.offreIdDe(id);
        try {
            adminClient.supprimerOffre(properties.typeId(), properties.versionNumber(), offreId);
        } catch (BcaasException e) {
            if (e.status() != 404) throw e;
        }
        try {
            medicamentService.supprimer(id);
        } catch (DataIntegrityViolationException e) {
            medicamentService.retirer(id);
        }
    }
}
