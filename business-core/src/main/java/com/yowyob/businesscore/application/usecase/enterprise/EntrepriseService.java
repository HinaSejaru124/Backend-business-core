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
                    return resoudreOrganisation(organizationId, nom).flatMap(orgId -> {
                        Entreprise entreprise = Entreprise.creer(
                                ctx.tenantId(), version.typeMetierId(), version.id(),
                                version.numero(), orgId, nom);
                        return depotEntreprise.sauvegarder(entreprise);
                    });
                });
    }

    /** Si aucun {@code organizationId} n'est fourni, provisionne l'organisation kernel + son agence. */
    private Mono<UUID> resoudreOrganisation(UUID organizationId, String nom) {
        if (organizationId != null) {
            return Mono.just(organizationId);
        }
        return persisterEntreprise.creerOrganisation(nom)
                .flatMap(orgId -> persisterEntreprise.creerAgence(orgId, nom + " — agence principale")
                        .thenReturn(orgId));
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
        return trouver(id, ctx).flatMap(entreprise ->
                depotEntreprise.sauvegarder(entreprise.changerCycleVie(cycleVie)));
    }
}
