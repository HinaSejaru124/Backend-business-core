package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.domain.port.out.OrganisationProvisionnee;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Provisionnement complet d'une organisation kernel — chaîne imposée par la gouvernance : résoudre le
 * business actor → créer l'organisation → l'approuver → souscrire les services → créer l'agence
 * principale. Extrait de {@link EntrepriseService} pour être réutilisé par le thème « Entreprise » :
 * une organisation par développeur (au lieu d'une par application), provisionnée soit à la création
 * d'une application (comportement historique, développeur déjà rattaché), soit explicitement via
 * {@code POST /v1/enterprise/provision} pour un développeur qui choisit le nom de son entreprise.
 */
@Service
public class OrganisationProvisioningService {

    private static final String MOTIF_APPROBATION_AUTO = "Approbation initiale";

    private final PersisterEntreprise persisterEntreprise;

    public OrganisationProvisioningService(PersisterEntreprise persisterEntreprise) {
        this.persisterEntreprise = persisterEntreprise;
    }

    /** Références kernel complètes d'une organisation fraîchement provisionnée. */
    public record RefsKernel(UUID businessActorId, UUID organizationId, UUID agencyId) {
    }

    public Mono<RefsKernel> provisionner(String nom) {
        return persisterEntreprise.creerOrganisation(nom)
                .flatMap(prov -> persisterEntreprise
                        .approuverOrganisation(prov.organizationId(), MOTIF_APPROBATION_AUTO)
                        .then(persisterEntreprise.souscrireServices(prov.organizationId()))
                        .then(persisterEntreprise.creerAgence(
                                prov.organizationId(), nom + " — agence principale"))
                        .map(agencyId -> new RefsKernel(
                                prov.businessActorId(), prov.organizationId(), agencyId)));
    }

    /** Rattache une Application à une organisation kernel déjà existante (sans la recréer). */
    public Mono<RefsKernel> rattacherExistante(UUID organizationId, String nom) {
        return persisterEntreprise.resoudreBusinessActorCourant(nom)
                .flatMap(businessActorId -> persisterEntreprise.trouverAgencePrincipale(organizationId)
                        .map(agencyId -> new RefsKernel(businessActorId, organizationId, agencyId))
                        .switchIfEmpty(Mono.just(new RefsKernel(businessActorId, organizationId, null))));
    }
}
