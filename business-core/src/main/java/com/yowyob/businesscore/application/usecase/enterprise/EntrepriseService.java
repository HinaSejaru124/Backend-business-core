package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case — gestion minimale des Entreprises (instances d'un Type Métier épinglées à une version).
 *
 * <p>Version de base fournie par la feature Opérations pour rendre l'exécution fonctionnelle de bout en
 * bout (résolution {@code businessId → versionTypeId / organizationId}). Périmètre Dev 3 : à compléter
 * (auto-provisionnement de l'organisation kernel, acteurs, offres).
 */
@Service
public class EntrepriseService {

    private final DepotEntreprise depotEntreprise;
    private final PersisterVersionType persisterVersionType;
    private final PersisterEntreprise persisterEntreprise;

    public EntrepriseService(DepotEntreprise depotEntreprise,
                             PersisterVersionType persisterVersionType,
                             PersisterEntreprise persisterEntreprise) {
        this.depotEntreprise = depotEntreprise;
        this.persisterVersionType = persisterVersionType;
        this.persisterEntreprise = persisterEntreprise;
    }

    public Mono<Entreprise> creer(UUID typeId, int numeroVersion, String nom,
                                  UUID organizationId, BusinessContext ctx) {
        return persisterVersionType.trouverParTypeEtNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMap(version -> {
                    version.verifierAppartenance(ctx.tenantId());
                    return resoudreOrganisation(organizationId, nom).flatMap(refs -> {
                        Entreprise entreprise = Entreprise.creer(
                                        ctx.tenantId(), version.typeMetierId(), version.id(),
                                        version.numero(), refs.organizationId(), nom)
                                .avecReferencesKernel(
                                        refs.businessActorId(), refs.organizationId(), refs.agencyId());
                        return depotEntreprise.sauvegarder(entreprise);
                    });
                });
    }

    /**
     * Si aucun {@code organizationId} n'est fourni, provisionne l'organisation kernel (onboarding du
     * business actor + organisation + agence principale) et renvoie les références à mémoriser.
     */
    private Mono<RefsKernel> resoudreOrganisation(UUID organizationId, String nom) {
        if (organizationId != null) {
            return Mono.just(new RefsKernel(null, organizationId, null));
        }
        return persisterEntreprise.creerOrganisation(nom)
                .flatMap(prov -> persisterEntreprise
                        .creerAgence(prov.organizationId(), nom + " — agence principale")
                        .map(agencyId -> new RefsKernel(
                                prov.businessActorId(), prov.organizationId(), agencyId)));
    }

    /** Références kernel à mémoriser dans l'entité Entreprise. */
    private record RefsKernel(UUID businessActorId, UUID organizationId, UUID agencyId) {
    }

    public Flux<Entreprise> lister(BusinessContext ctx) {
        return depotEntreprise.listerParTenant(ctx.tenantId());
    }

    public Mono<Entreprise> trouver(UUID id, BusinessContext ctx) {
        return depotEntreprise.trouverParId(id)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Entreprise introuvable : " + id)))
                .doOnNext(entreprise -> entreprise.verifierAppartenance(ctx.tenantId()));
    }

    public Mono<Entreprise> changerCycleVie(UUID id, CycleVie cycleVie, BusinessContext ctx) {
        return trouver(id, ctx).flatMap(entreprise -> {
            // Propage la transition au kernel (approve/suspend/close/reopen) avant de persister localement.
            Mono<Void> transitionKernel = entreprise.organizationId() == null
                    ? Mono.empty()
                    : persisterEntreprise.changerCycleVieKernel(entreprise.organizationId(), cycleVie);
            return transitionKernel.then(
                    depotEntreprise.sauvegarder(entreprise.changerCycleVie(cycleVie)));
        });
    }
}
