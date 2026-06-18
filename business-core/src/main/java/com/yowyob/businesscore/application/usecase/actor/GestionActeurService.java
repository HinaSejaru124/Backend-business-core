package com.yowyob.businesscore.application.usecase.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur;
import com.yowyob.businesscore.domain.port.out.actor.AppliquerRoleTechnique;
import com.yowyob.businesscore.domain.port.out.actor.DepotActeur;
import com.yowyob.businesscore.domain.port.out.actor.RattacherAOrganisation;
import com.yowyob.businesscore.domain.port.out.actor.ResoudreBeneficiaire;
import com.yowyob.businesscore.domain.port.out.actor.ResoudrePersonne;
import com.yowyob.businesscore.domain.port.out.enterprise.LireEntreprise;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import com.yowyob.businesscore.shared.error.ProblemException;
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
public class GestionActeurService implements GestionActeur {

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

    @Override
    public Mono<RoleMetier> declarerRole(DeclarerRoleCommande commande) {
        RoleMetier role = RoleMetier.nouveau(
                UUID.randomUUID(), commande.versionTypeId(), commande.code(), commande.categorie());
        return depot.enregistrerRole(role);
    }

    @Override
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
                ? resoudrePersonne.resoudreOperateur(identifiant)
                .flatMap(actorId -> appliquerRoleTechnique.appliquer(role.code(), actorId)
                        .thenReturn(actorId))
                : resoudreBeneficiaire.resoudreBeneficiaire(identifiant);

        return kernelId.flatMap(acteurKernelId ->
                rattacherAOrganisation.rattacher(entreprise.organizationId(), acteurKernelId)
                        .then(depot.enregistrerActeur(ActeurMetier.nouveau(
                                UUID.randomUUID(), entreprise.id(), role.id(), acteurKernelId))));
    }

    @Override
    public Flux<ActeurMetier> lister(UUID businessId) {
        return depot.acteursParEntreprise(businessId);
    }

    @Override
    public Mono<Void> detacher(UUID businessId, UUID actorId) {
        return depot.acteurParId(actorId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Acteur introuvable : " + actorId)))
                .filter(a -> a.entrepriseId().equals(businessId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Acteur " + actorId + " non rattaché à l'entreprise " + businessId)))
                .map(a -> a.detacher(Instant.now()))
                .flatMap(depot::enregistrerActeur)
                .then();
    }
}