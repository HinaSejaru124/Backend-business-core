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
     * Provisionne l'organisation kernel si aucun {@code organizationId} n'est fourni.
     *
     * <p>Chaîne kernel (ordre imposé par la gouvernance) :
     * <ol>
     *   <li>Résoudre le business actor (onboarding ou profil existant),</li>
     *   <li>Créer l'organisation,</li>
     *   <li>Approuver l'organisation ({@code POST .../approve}),</li>
     *   <li>Souscrire les services kernel,</li>
     *   <li>Créer l'agence principale.</li>
     * </ol>
     */
    private static final String MOTIF_APPROBATION_AUTO = "Approbation initiale";

    private Mono<RefsKernel> resoudreOrganisation(UUID organizationId, String nom) {
        if (organizationId != null) {
            return Mono.just(new RefsKernel(null, organizationId, null));
        }
        return persisterEntreprise.creerOrganisation(nom)
                .flatMap(prov -> persisterEntreprise
                        .approuverOrganisation(prov.organizationId(), MOTIF_APPROBATION_AUTO)
                        .then(persisterEntreprise.souscrireServices(prov.organizationId()))
                        .then(persisterEntreprise.creerAgence(
                                prov.organizationId(), nom + " — agence principale"))
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
            // Propage la transition au kernel (suspend/close/reopen) avant de persister localement.
            Mono<Void> transitionKernel = entreprise.organizationId() == null
                    ? Mono.empty()
                    : persisterEntreprise.changerCycleVieKernel(entreprise.organizationId(), cycleVie);
            return transitionKernel.then(
                    depotEntreprise.sauvegarder(entreprise.changerCycleVie(cycleVie)));
        });
    }

    /**
     * Première approbation de gouvernance kernel, puis passage local en {@link CycleVie#ACTIVE}.
     * Distinct de {@link #changerCycleVie} avec ACTIVE (qui appelle {@code reopen}).
     */
    public Mono<Entreprise> approuver(UUID id, String reason, BusinessContext ctx) {
        return trouver(id, ctx).flatMap(entreprise -> {
            if (entreprise.organizationId() == null) {
                return Mono.error(ProblemException.unprocessable(
                        "L'entreprise n'a pas d'organisation kernel à approuver."));
            }
            return persisterEntreprise.approuverOrganisation(entreprise.organizationId(), reason)
                    .then(depotEntreprise.sauvegarder(entreprise.changerCycleVie(CycleVie.ACTIVE)));
        });
    }

    /** Met à jour le nom local de l'entreprise (pas de rename kernel). */
    public Mono<Entreprise> modifier(UUID id, String nom, BusinessContext ctx) {
        return trouver(id, ctx)
                .map(entreprise -> entreprise.renommer(nom))
                .flatMap(depotEntreprise::sauvegarder);
    }

    /** Archive l'entreprise : cycle de vie {@link CycleVie#FERMEE} (local + kernel {@code close}). */
    public Mono<Void> archiver(UUID id, BusinessContext ctx) {
        return changerCycleVie(id, CycleVie.FERMEE, ctx).then();
    }
}
