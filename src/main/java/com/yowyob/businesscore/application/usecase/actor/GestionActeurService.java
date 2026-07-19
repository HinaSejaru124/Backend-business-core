package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.AppliquerRoleTechnique;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.port.out.RattacherAOrganisation;
import com.yowyob.businesscore.domain.port.out.ResoudreBeneficiaire;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestration des acteurs métier.
 *
 * <p>Kernel Core est la seule source de vérité de l'identité — Business Core ne résout ni ne crée
 * jamais un utilisateur kernel à partir d'un simple identifiant texte (email, nom...). Deux chemins :
 * <ul>
 *   <li>{@link #rattacher} — un développeur rattache un acteur <b>déjà connu</b> (son
 *       {@code acteurKernelId}, obtenu ailleurs — un autre Business, une future recherche kernel...).
 *       Aucun appel kernel de création d'identité ici.</li>
 *   <li>{@link #inscrire} — libre-service : la personne fournit ses identifiants, Business Core délègue
 *       entièrement au kernel (login, puis sign-up seulement si le compte n'existe pas encore).</li>
 * </ul>
 * RG-04 : un bénéficiaire (client, tiers) n'est pas un utilisateur kernel qui se connecte — il reste
 * résolu/créé comme ressource métier légère via {@link ResoudreBeneficiaire}, chemin inchangé et étanche
 * de celui d'un opérateur.
 */
@Service
public class GestionActeurService {

    public record DeclarerRoleCommande(UUID versionTypeId, String code, CategorieActeur categorie) {}

    /**
     * {@code acteurKernelId} : requis pour un rôle OPERATEUR — identité déjà connue, Business Core ne la
     * résout pas. {@code identifiantPersonne} : requis pour un rôle BENEFICIAIRE — RG-04, chemin inchangé.
     */
    public record RattacherActeurCommande(
            UUID businessId, UUID roleMetierId, UUID acteurKernelId, String identifiantPersonne) {}

    public record ModifierActeurCommande(UUID businessId, UUID actorId, UUID nouveauRoleMetierId) {}

    public record ModifierRoleCommande(UUID versionTypeId, UUID roleId, String code) {}

    /** Inscription libre-service d'un acteur (email/mot de passe) — distincte de {@link #rattacher}. */
    public record InscrireActeurCommande(
            UUID businessId, UUID roleMetierId, String email, String password, String firstName, String lastName) {}

    private final DepotActeur depot;
    private final LireEntreprise lireEntreprise;
    private final ResoudreBeneficiaire resoudreBeneficiaire;
    private final AppliquerRoleTechnique appliquerRoleTechnique;
    private final RattacherAOrganisation rattacherAOrganisation;
    private final AuthentifierUtilisateur authentifier;
    private final PersisterVersionType persisterVersionType;

    public GestionActeurService(DepotActeur depot,
                                LireEntreprise lireEntreprise,
                                ResoudreBeneficiaire resoudreBeneficiaire,
                                AppliquerRoleTechnique appliquerRoleTechnique,
                                RattacherAOrganisation rattacherAOrganisation,
                                AuthentifierUtilisateur authentifier,
                                PersisterVersionType persisterVersionType) {
        this.depot = depot;
        this.lireEntreprise = lireEntreprise;
        this.resoudreBeneficiaire = resoudreBeneficiaire;
        this.appliquerRoleTechnique = appliquerRoleTechnique;
        this.rattacherAOrganisation = rattacherAOrganisation;
        this.authentifier = authentifier;
        this.persisterVersionType = persisterVersionType;
    }

    /** RG-03 : une version publiée (immuable) ne peut plus voir ses rôles métier créés/modifiés/supprimés. */
    private Mono<Void> verifierVersionModifiable(UUID versionTypeId) {
        return persisterVersionType.trouverParId(versionTypeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Version introuvable : " + versionTypeId)))
                .doOnNext(VersionType::verifierModifiable)
                .then();
    }

    public Mono<RoleMetier> declarerRole(DeclarerRoleCommande commande) {
        return verifierVersionModifiable(commande.versionTypeId())
                .then(Mono.defer(() -> {
                    RoleMetier role = RoleMetier.nouveau(
                            UUID.randomUUID(), commande.versionTypeId(), commande.code(), commande.categorie());
                    return depot.enregistrerRole(role);
                }));
    }

    public Flux<RoleMetier> listerRoles(UUID versionTypeId) {
        return depot.rolesParVersionType(versionTypeId);
    }

    public Mono<RoleMetier> modifierRole(ModifierRoleCommande commande) {
        return verifierVersionModifiable(commande.versionTypeId())
                .then(depot.roleParId(commande.roleId())
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Rôle métier introuvable : " + commande.roleId()))))
                .filter(r -> r.versionTypeId().equals(commande.versionTypeId()))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle " + commande.roleId() + " absent de cette version")))
                .map(r -> r.avecCode(commande.code()))
                .flatMap(depot::enregistrerRole);
    }

    public Mono<Void> supprimerRole(UUID versionTypeId, UUID roleId) {
        return verifierVersionModifiable(versionTypeId)
                .then(depot.roleParId(roleId)
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Rôle métier introuvable : " + roleId))))
                .filter(r -> r.versionTypeId().equals(versionTypeId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle " + roleId + " absent de cette version")))
                .flatMap(role -> depot.compterActeursActifsParRole(role.id())
                        .flatMap(count -> {
                            if (count > 0) {
                                return Mono.error(ProblemException.conflict(
                                        "Impossible de supprimer le rôle : "
                                                + count + " acteur(s) actif(s) le référencent.")
                                        .violatedRule("ROLE_EN_COURS_D_USAGE"));
                            }
                            return depot.supprimerRole(role.id());
                        }));
    }

    /**
     * Rattache un acteur <b>déjà connu</b> à cette entreprise. Business Core ne résout ni ne crée aucune
     * identité kernel ici : pour un OPERATEUR, le développeur fournit directement l'{@code acteurKernelId}
     * (obtenu ailleurs — Kernel fait autorité sur l'identité). Pour un BENEFICIAIRE (RG-04), chemin
     * inchangé : ce n'est pas un utilisateur qui se connecte, {@link ResoudreBeneficiaire} continue de
     * résoudre/créer la ressource tiers à partir de l'identifiant fourni.
     */
    public Mono<ActeurMetier> rattacher(RattacherActeurCommande commande) {
        Mono<Entreprise> entreprise = lireEntreprise.parId(commande.businessId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Application introuvable : " + commande.businessId())));

        Mono<RoleMetier> role = depot.roleParId(commande.roleMetierId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle métier introuvable : " + commande.roleMetierId())));

        return Mono.zip(entreprise, role).flatMap(t -> resoudreEtRattacher(t.getT1(), t.getT2(), commande));
    }

    private Mono<ActeurMetier> resoudreEtRattacher(Entreprise entreprise, RoleMetier role,
                                                    RattacherActeurCommande commande) {
        if (role.categorie() == CategorieActeur.OPERATEUR) {
            if (commande.acteurKernelId() == null) {
                return Mono.error(ProblemException.badRequest(
                        "acteurKernelId est requis pour un rôle OPERATEUR : Business Core ne résout plus "
                                + "d'identité à partir d'un identifiant texte, Kernel Core fait autorité. "
                                + "Un utilisateur inconnu doit créer lui-même son compte via "
                                + ":register."));
            }
            return appliquerRoleTechnique.appliquer(commande.acteurKernelId(), role.code())
                    .then(rattacherEtEnregistrer(entreprise, role, commande.acteurKernelId()));
        }
        if (commande.identifiantPersonne() == null || commande.identifiantPersonne().isBlank()) {
            return Mono.error(ProblemException.badRequest(
                    "identifiantPersonne est requis pour un rôle BENEFICIAIRE."));
        }
        return resoudreBeneficiaire
                .resoudreBeneficiaire(entreprise.organizationId(), commande.identifiantPersonne(),
                        commande.identifiantPersonne())
                .flatMap(acteurKernelId -> rattacherEtEnregistrer(entreprise, role, acteurKernelId));
    }

    /**
     * Inscription libre-service d'un acteur : <b>un seul compte Yow</b>, réutilisable dans N entreprises.
     * Kernel Core fait autorité sur l'identité — Business Core délègue entièrement :
     * <ol>
     *   <li>tente une connexion avec les identifiants fournis ;</li>
     *   <li>si elle réussit, le compte existe déjà (développeur, ou déjà acteur ailleurs) — son
     *       {@code actorId} est réutilisé tel quel, aucun sign-up ;</li>
     *   <li>si elle échoue, la personne est nouvelle — sign-up puis nouvelle connexion pour récupérer
     *       l'{@code actorId} qui sera utilisé à chaque connexion future ;</li>
     *   <li>rattachement (ou récupération du rattachement existant) à cette entreprise.</li>
     * </ol>
     * Réservé aux rôles OPERATEUR (RG-04) : un bénéficiaire n'a pas d'identifiants de connexion, il reste
     * rattaché par le développeur via {@link #rattacher}.
     */
    public Mono<ActeurMetier> inscrire(InscrireActeurCommande commande) {
        Mono<Entreprise> entreprise = lireEntreprise.parId(commande.businessId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Application introuvable : " + commande.businessId())));
        Mono<RoleMetier> role = depot.roleParId(commande.roleMetierId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle métier introuvable : " + commande.roleMetierId())));

        return Mono.zip(entreprise, role).flatMap(t -> inscrireEtRattacher(t.getT1(), t.getT2(), commande));
    }

    private Mono<ActeurMetier> inscrireEtRattacher(Entreprise entreprise, RoleMetier role,
                                                    InscrireActeurCommande commande) {
        if (role.categorie() != CategorieActeur.OPERATEUR) {
            return Mono.error(ProblemException.unprocessable(
                    "L'inscription libre-service n'est proposée qu'aux rôles OPERATEUR (RG-04) : un "
                            + "bénéficiaire est rattaché par le développeur via POST .../actors, sans "
                            + "identifiants de connexion.")
                    .violatedRule("CATEGORIE_ROLE_INVALIDE"));
        }
        return resoudreIdentiteKernel(commande.email(), commande.password(), commande.firstName(),
                        commande.lastName())
                .flatMap(acteurKernelId -> appliquerRoleTechnique.appliquer(acteurKernelId, role.code())
                        .thenReturn(acteurKernelId))
                .flatMap(acteurKernelId -> rattacherEtEnregistrer(entreprise, role, acteurKernelId));
    }

    /**
     * Connexion d'abord, sign-up seulement si le compte n'existe pas — jamais l'inverse. Si le sign-up
     * vient de réussir mais qu'une connexion immédiate échoue encore (ex. vérification d'email requise
     * par le kernel), on ne fabrique pas d'identité de repli : le compte est créé, la personne doit
     * simplement retenter (ou finaliser sa vérification) avant que le rattachement puisse aboutir.
     */
    private Mono<UUID> resoudreIdentiteKernel(String email, String password, String firstName, String lastName) {
        return tenterConnexion(email, password)
                .flatMap(actorId -> actorId.isPresent()
                        ? Mono.just(actorId.get())
                        : authentifier.signUp(email, password, firstName, lastName)
                                .then(tenterConnexion(email, password))
                                .flatMap(apresSignUp -> apresSignUp.isPresent()
                                        ? Mono.just(apresSignUp.get())
                                        : Mono.error(ProblemException.badGateway(
                                                "Compte créé, mais son identité n'a pas pu être confirmée "
                                                        + "immédiatement (vérification d'email requise par le "
                                                        + "kernel ?). Réessayez une fois le compte activé."))));
    }

    private Mono<Optional<UUID>> tenterConnexion(String email, String password) {
        return authentifier.login(email, password)
                .map(session -> Optional.ofNullable(parseUuid(session.actorId())))
                .onErrorReturn(Optional.empty());
    }

    /**
     * Refuse un second rattachement actif pour le même couple (acteur kernel, entreprise) — un compte
     * Yow ne doit avoir qu'une ligne {@code acteur_metier} active par entreprise (contrainte reflétée en
     * base par l'index unique partiel, cf. changelog {@code acteur-unicite.xml}).
     */
    private Mono<ActeurMetier> rattacherEtEnregistrer(Entreprise entreprise, RoleMetier role, UUID acteurKernelId) {
        return depot.acteursParEntreprise(entreprise.id())
                .filter(a -> a.estActif() && a.acteurKernelId().equals(acteurKernelId))
                .hasElements()
                .flatMap(dejaRattache -> {
                    if (dejaRattache) {
                        return Mono.error(ProblemException.conflict(
                                "Cet acteur est déjà rattaché activement à cette application.")
                                .violatedRule("ACTEUR_DEJA_RATTACHE"));
                    }
                    return rattacherAOrganisation.rattacher(entreprise.organizationId(), acteurKernelId)
                            .then(depot.enregistrerActeur(ActeurMetier.nouveau(
                                    UUID.randomUUID(), entreprise.id(), role.id(), acteurKernelId)));
                });
    }

    private static UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Flux<ActeurMetier> lister(UUID businessId) {
        return depot.acteursParEntreprise(businessId);
    }

    public Mono<ActeurMetier> trouver(UUID businessId, UUID actorId) {
        return depot.acteurParId(actorId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Acteur introuvable : " + actorId)))
                .filter(a -> a.entrepriseId().equals(businessId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Acteur " + actorId + " non rattaché à l'application " + businessId)));
    }

    /**
     * Change le rôle d'un acteur actif : détache l'association courante et en crée une nouvelle
     * (RG-04 — on ne mute pas la catégorie ; le nouveau rôle doit être de même catégorie).
     */
    public Mono<ActeurMetier> modifier(ModifierActeurCommande commande) {
        return trouver(commande.businessId(), commande.actorId())
                .flatMap(existant -> {
                    if (!existant.estActif()) {
                        return Mono.error(ProblemException.conflict(
                                "Impossible de modifier un acteur déjà détaché.")
                                .violatedRule("ACTEUR_DETACHE"));
                    }
                    return depot.roleParId(existant.roleMetierId())
                            .zipWith(depot.roleParId(commande.nouveauRoleMetierId())
                                    .switchIfEmpty(Mono.error(ProblemException.notFound(
                                            "Rôle métier introuvable : " + commande.nouveauRoleMetierId()))))
                            .flatMap(roles -> {
                                RoleMetier ancien = roles.getT1();
                                RoleMetier nouveau = roles.getT2();
                                if (ancien.categorie() != nouveau.categorie()) {
                                    return Mono.error(ProblemException.unprocessable(
                                            "Changement de catégorie interdit (RG-04) : "
                                                    + ancien.categorie() + " → " + nouveau.categorie()));
                                }
                                return depot.enregistrerActeur(existant.detacher(Instant.now()))
                                        .then(lireEntreprise.parId(commande.businessId())
                                                .switchIfEmpty(Mono.error(ProblemException.notFound(
                                                        "Application introuvable : " + commande.businessId()))))
                                        .flatMap(entreprise -> {
                                            Mono<Void> roleKernel = nouveau.categorie() == CategorieActeur.OPERATEUR
                                                    ? appliquerRoleTechnique.appliquer(
                                                            existant.acteurKernelId(), nouveau.code())
                                                    : Mono.empty();
                                            return roleKernel.then(depot.enregistrerActeur(
                                                    ActeurMetier.nouveau(
                                                            UUID.randomUUID(),
                                                            entreprise.id(),
                                                            nouveau.id(),
                                                            existant.acteurKernelId())));
                                        });
                            });
                });
    }

    public Mono<Void> detacher(UUID businessId, UUID actorId) {
        return trouver(businessId, actorId)
                .map(a -> a.detacher(Instant.now()))
                .flatMap(depot::enregistrerActeur)
                .then();
    }
}
