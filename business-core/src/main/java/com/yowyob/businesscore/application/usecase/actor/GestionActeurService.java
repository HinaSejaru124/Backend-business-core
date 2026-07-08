package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.AppliquerRoleTechnique;
import com.yowyob.businesscore.domain.port.out.RattacherAOrganisation;
import com.yowyob.businesscore.domain.port.out.ResoudreBeneficiaire;
import com.yowyob.businesscore.domain.port.out.ResoudrePersonne;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Orchestration des acteurs métier. RG-04 : opérateur et bénéficiaire passent par des cores
 * kernel DIFFÉRENTS et restent étanches — la catégorie du rôle métier détermine le chemin.
 */
@Service
public class GestionActeurService {

    public record DeclarerRoleCommande(UUID versionTypeId, String code, CategorieActeur categorie) {}

    public record RattacherActeurCommande(UUID businessId, UUID roleMetierId, String identifiantPersonne) {}

    public record ModifierActeurCommande(UUID businessId, UUID actorId, UUID nouveauRoleMetierId) {}

    public record ModifierRoleCommande(UUID versionTypeId, UUID roleId, String code) {}

    private final DepotActeur depot;
    private final LireEntreprise lireEntreprise;
    private final ResoudrePersonne resoudrePersonne;
    private final ResoudreBeneficiaire resoudreBeneficiaire;
    private final AppliquerRoleTechnique appliquerRoleTechnique;
    private final RattacherAOrganisation rattacherAOrganisation;

    public GestionActeurService(DepotActeur depot,
                                LireEntreprise lireEntreprise,
                                ResoudrePersonne resoudrePersonne,
                                ResoudreBeneficiaire resoudreBeneficiaire,
                                AppliquerRoleTechnique appliquerRoleTechnique,
                                RattacherAOrganisation rattacherAOrganisation) {
        this.depot = depot;
        this.lireEntreprise = lireEntreprise;
        this.resoudrePersonne = resoudrePersonne;
        this.resoudreBeneficiaire = resoudreBeneficiaire;
        this.appliquerRoleTechnique = appliquerRoleTechnique;
        this.rattacherAOrganisation = rattacherAOrganisation;
    }

    public Mono<RoleMetier> declarerRole(DeclarerRoleCommande commande) {
        RoleMetier role = RoleMetier.nouveau(
                UUID.randomUUID(), commande.versionTypeId(), commande.code(), commande.categorie());
        return depot.enregistrerRole(role);
    }

    public Flux<RoleMetier> listerRoles(UUID versionTypeId) {
        return depot.rolesParVersionType(versionTypeId);
    }

    public Mono<RoleMetier> modifierRole(ModifierRoleCommande commande) {
        return depot.roleParId(commande.roleId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle métier introuvable : " + commande.roleId())))
                .filter(r -> r.versionTypeId().equals(commande.versionTypeId()))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle " + commande.roleId() + " absent de cette version")))
                .map(r -> r.avecCode(commande.code()))
                .flatMap(depot::enregistrerRole);
    }

    public Mono<Void> supprimerRole(UUID versionTypeId, UUID roleId) {
        return depot.roleParId(roleId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle métier introuvable : " + roleId)))
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

    public Mono<ActeurMetier> rattacher(RattacherActeurCommande commande) {
        Mono<Entreprise> entreprise = lireEntreprise.parId(commande.businessId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + commande.businessId())));

        Mono<RoleMetier> role = depot.roleParId(commande.roleMetierId())
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Rôle métier introuvable : " + commande.roleMetierId())));

        return Mono.zip(entreprise, role)
                .flatMap(t -> resoudreEtRattacher(t.getT1(), t.getT2(), commande.identifiantPersonne()));
    }

    private Mono<ActeurMetier> resoudreEtRattacher(Entreprise entreprise, RoleMetier role, String identifiant) {
        // Chemin étanche selon la catégorie (RG-04).
        Mono<UUID> kernelId = (role.categorie() == CategorieActeur.OPERATEUR)
                ? resoudrePersonne.resoudreOperateur(identifiant, identifiant)
                .flatMap(actorId -> appliquerRoleTechnique.appliquer(actorId, role.code())
                        .thenReturn(actorId))
                : resoudreBeneficiaire.resoudreBeneficiaire(
                        entreprise.organizationId(), identifiant, identifiant);

        return kernelId.flatMap(acteurKernelId ->
                rattacherAOrganisation.rattacher(entreprise.organizationId(), acteurKernelId)
                        .then(depot.enregistrerActeur(ActeurMetier.nouveau(
                                UUID.randomUUID(), entreprise.id(), role.id(), acteurKernelId))));
    }

    public Flux<ActeurMetier> lister(UUID businessId) {
        return depot.acteursParEntreprise(businessId);
    }

    public Mono<ActeurMetier> trouver(UUID businessId, UUID actorId) {
        return depot.acteurParId(actorId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Acteur introuvable : " + actorId)))
                .filter(a -> a.entrepriseId().equals(businessId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Acteur " + actorId + " non rattaché à l'entreprise " + businessId)));
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
                                                        "Entreprise introuvable : " + commande.businessId()))))
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
