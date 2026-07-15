package com.pharmacore.pharmaciebackend.personnel;

import com.pharmacore.pharmaciebackend.admin.BcaasAdminClient;
import com.pharmacore.pharmaciebackend.auth.PharmacoreSession;
import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import com.pharmacore.pharmaciebackend.config.BcaasProperties;
import com.pharmacore.pharmaciebackend.config.RessourceIntrouvableException;
import com.pharmacore.pharmaciebackend.personnel.PersonnelDtos.CreerPersonnelRequest;
import com.pharmacore.pharmaciebackend.personnel.PersonnelDtos.PersonnelResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestion du personnel (Pharmacien Responsable, Caissier) — comptes propres à PharmaCore.
 *
 * <p>Business Core exige un {@code acteurKernelId} réel pour tout appel {@code Vendre:execute}
 * ({@code X-BC-On-Behalf-Of}) — ce n'est pas contournable, c'est le mécanisme réel de traçabilité de la
 * plateforme. Plutôt que d'inscrire une identité kernel <b>par employé</b> ({@code actors:register},
 * qui exige une vérification d'email — trop fragile pour une appli de démo, cf. FEUILLE-DE-ROUTE.md),
 * on rattache l'identité kernel <b>déjà vérifiée</b> du titulaire ({@link PharmacoreSession#titulaireActorIdOuNull})
 * au rôle demandé, via le rattachement classique (brique 3, {@code POST /v1/businesses/{id}/actors}) —
 * aucune inscription, aucun email, immédiat. Cette identité est réutilisée pour tout le personnel d'un
 * même rôle : la connexion quotidienne, elle, reste 100% locale à PharmaCore (email/mot de passe propres,
 * jamais transmis au kernel).
 *
 * <p><b>Limite honnête, à ne pas cacher</b> : n'ayant qu'une seule identité kernel déjà vérifiée
 * disponible (celle du titulaire), Pharmacien Responsable et Caissier sont <i>tous deux</i> rattachés à
 * cette même identité (avec deux rattachements de rôle différents dessus). Business Core résout donc
 * l'ensemble des rôles de cette identité (CAISSIER <b>et</b> PHARMACIEN_RESPONSABLE) quel que soit le
 * compte PharmaCore utilisé pour se connecter — la dérogation « ordonnance requise » n'est donc plus
 * réellement bloquée pour un Caissier au niveau de Business Core dans cette configuration. Ce qui reste
 * réel et correctement différencié, c'est l'espace PharmaCore (pages visibles, bouton de dérogation
 * affiché) selon le compte local connecté — suffisant pour une démonstration de navigation par rôle,
 * insuffisant pour une vraie séparation de droits en production (qui demanderait une deuxième identité
 * kernel vérifiée, aujourd'hui bloquée par la vérification d'email indisponible).
 */
@Service
public class PersonnelService {

    private final PersonnelRepository repository;
    private final BcaasAdminClient adminClient;
    private final PharmacoreSession session;
    private final BcaasProperties properties;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public PersonnelService(PersonnelRepository repository, BcaasAdminClient adminClient,
                            PharmacoreSession session, BcaasProperties properties) {
        this.repository = repository;
        this.adminClient = adminClient;
        this.session = session;
        this.properties = properties;
    }

    @Transactional
    public PersonnelResponse creer(CreerPersonnelRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new BcaasException(409, "Compte déjà existant",
                    "Un membre du personnel utilise déjà l'adresse " + req.email() + ".", null, null, null);
        }

        UUID acteurKernelId = resoudreOuRattacherActeurRole(req.role());

        Personnel personnel = repository.save(new Personnel(
                req.nom(), req.prenom(), req.email(), encoder.encode(req.motDePasse()), req.role(),
                acteurKernelId));

        return PersonnelResponse.depuis(personnel);
    }

    /**
     * Un membre du personnel existe-t-il déjà pour ce rôle ? Si oui, on réutilise son
     * {@code acteurKernelId} (aucun appel kernel). Sinon, on rattache l'identité kernel du titulaire
     * (déjà vérifiée) à ce rôle — la seule fois où ce rôle touche le kernel.
     */
    private UUID resoudreOuRattacherActeurRole(String role) {
        Optional<Personnel> premierDuRole = repository.findFirstByRoleOrderByCreeLeAsc(role);
        if (premierDuRole.isPresent()) {
            return premierDuRole.get().getActeurKernelId();
        }

        String titulaireActorId = session.titulaireActorIdOuNull();
        if (titulaireActorId == null) {
            throw new BcaasException(401, "Non connecté",
                    "Session titulaire introuvable — reconnectez-vous avant de créer du personnel.",
                    null, null, null);
        }

        UUID roleMetierId = resoudreRoleMetierId(role);
        try {
            adminClient.rattacherActeurOperateur(properties.businessId(), roleMetierId.toString(), titulaireActorId);
        } catch (BcaasException e) {
            if (e.status() != 409) throw e; // 409 = déjà rattaché à ce rôle, rien à faire
        }
        return UUID.fromString(titulaireActorId);
    }

    public List<PersonnelResponse> lister() {
        return repository.findAllByOrderByCreeLeDesc().stream().map(PersonnelResponse::depuis).toList();
    }

    public void desactiver(UUID id) {
        Personnel personnel = repository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Personnel", id));
        personnel.desactiver();
        repository.save(personnel);
    }

    /** Vérifie l'email/mot de passe local ; ne touche jamais le kernel (cf. javadoc de classe). */
    public Optional<Personnel> authentifier(String email, String motDePasse) {
        return repository.findByEmailAndActifTrue(email)
                .filter(p -> encoder.matches(motDePasse, p.getMotDePasseHash()));
    }

    private UUID resoudreRoleMetierId(String code) {
        return adminClient.listerRoles(properties.typeId(), properties.versionNumber()).stream()
                .filter(r -> code.equals(String.valueOf(r.get("code"))))
                .map(r -> UUID.fromString(String.valueOf(r.get("id"))))
                .findFirst()
                .orElseThrow(() -> new BcaasException(422, "Rôle non provisionné",
                        "Le rôle " + code + " n'existe pas encore côté Business Core — "
                                + "provisionnez le modèle avant de créer du personnel.", null, null, null));
    }
}
