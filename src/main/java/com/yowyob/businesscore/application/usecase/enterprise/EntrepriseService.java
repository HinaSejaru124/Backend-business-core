package com.yowyob.businesscore.application.usecase.enterprise;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.enterprise.EntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.EntrepriseProfil;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntreprise;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseContrat;
import com.yowyob.businesscore.domain.enterprise.spi.DepotEntrepriseProfil;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync.OperationSync;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync.TypeEntiteSync;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.shared.CycleVie;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
    private final JournaliserChangementSync journaliserSync;
    private final DepotEntrepriseContrat depotEntrepriseContrat;
    private final DepotEntrepriseProfil depotEntrepriseProfil;
    private final DeveloperAccountRepository developerRepository;
    private final PlanCatalogue catalogue;
    private final OrganisationProvisioningService organisationProvisioningService;

    public EntrepriseService(DepotEntreprise depotEntreprise,
                             PersisterVersionType persisterVersionType,
                             PersisterEntreprise persisterEntreprise,
                             JournaliserChangementSync journaliserSync,
                             DepotEntrepriseContrat depotEntrepriseContrat,
                             DepotEntrepriseProfil depotEntrepriseProfil,
                             DeveloperAccountRepository developerRepository,
                             PlanCatalogue catalogue,
                             OrganisationProvisioningService organisationProvisioningService) {
        this.depotEntreprise = depotEntreprise;
        this.persisterVersionType = persisterVersionType;
        this.persisterEntreprise = persisterEntreprise;
        this.journaliserSync = journaliserSync;
        this.depotEntrepriseContrat = depotEntrepriseContrat;
        this.depotEntrepriseProfil = depotEntrepriseProfil;
        this.developerRepository = developerRepository;
        this.catalogue = catalogue;
        this.organisationProvisioningService = organisationProvisioningService;
    }

    /**
     * Applique la limite d'applications du forfait du développeur (métrique hybride, fixée par l'admin).
     * {@code applicationsMax < 0} = illimité. <b>Fail-open</b> : si le compte/plan n'est pas résoluble,
     * on n'empêche jamais la création (aucun blocage sur une donnée manquante).
     */
    private Mono<Void> verifierLimiteApplications(BusinessContext ctx) {
        return developerRepository.findByKernelTenantId(ctx.tenantId())
                .flatMap(dev -> {
                    long max = catalogue.applicationsMax(dev.getPlan());
                    if (max < 0) {
                        return Mono.empty();
                    }
                    return depotEntreprise.listerParTenant(ctx.tenantId()).count()
                            .flatMap(n -> n >= max
                                    ? Mono.error(ProblemException.unprocessable(
                                            "Limite d'applications atteinte pour le forfait "
                                                    + catalogue.normaliser(dev.getPlan()) + " (" + max
                                                    + " maximum). Passez à un forfait supérieur pour en créer davantage.")
                                            .violatedRule("LIMITE_APPLICATIONS"))
                                    : Mono.empty());
                })
                .then();
    }

    /** Journalise un changement d'entreprise pour la synchronisation pull des backends terminaux. */
    private Mono<Entreprise> journaliser(Entreprise entreprise, OperationSync operation) {
        return journaliserSync.journaliser(entreprise.tenantId(), entreprise.id(), TypeEntiteSync.ENTREPRISE,
                        entreprise.id(), operation, entreprise)
                .thenReturn(entreprise);
    }

    /**
     * Crée une Application (logiciel) au sein de l'entreprise du développeur.
     *
     * <p>Thème strict de la plateforme : un développeur travaille pour <b>une seule</b> entreprise
     * (= une organisation kernel), à l'intérieur de laquelle il crée ses applications — jamais une
     * organisation par application. L'organisation cible n'est donc plus fournie par l'appelant : elle
     * est résolue depuis {@code developer_account.entreprise_organization_id} (fixée une fois pour
     * toutes via {@code POST /v1/enterprise/provision} ou {@code /select}). Aucune entreprise choisie
     * → 422 explicite (le développeur doit d'abord passer par la sélection).
     */
    public Mono<Entreprise> creer(UUID typeId, int numeroVersion, String nom, BusinessContext ctx) {
        return verifierLimiteApplications(ctx).then(resoudreOrganisationDuDeveloppeur(ctx)
                .flatMap(organizationId -> persisterVersionType.trouverParTypeEtNumero(typeId, numeroVersion)
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                        .flatMap(version -> {
                            version.verifierAppartenance(ctx.tenantId());
                            return organisationProvisioningService.rattacherExistante(organizationId, nom)
                                    .flatMap(refs -> {
                                        Entreprise entreprise = Entreprise.creer(
                                                        ctx.tenantId(), version.typeMetierId(), version.id(),
                                                        version.numero(), refs.organizationId(), nom)
                                                .avecReferencesKernel(
                                                        refs.businessActorId(), refs.organizationId(), refs.agencyId());
                                        return depotEntreprise.sauvegarder(entreprise)
                                                .flatMap(saved -> depotEntrepriseContrat
                                                        .sauvegarder(EntrepriseContrat.vierge(
                                                                saved.id(), saved.tenantId(), Instant.now()))
                                                        .thenReturn(saved))
                                                .flatMap(saved -> depotEntrepriseProfil
                                                        .sauvegarder(EntrepriseProfil.vierge(saved.id(), saved.tenantId(), Instant.now()))
                                                        .thenReturn(saved))
                                                .flatMap(saved -> journaliser(saved, OperationSync.CREATE));
                                    });
                        })));
    }

    /** Résout l'organisation kernel de l'entreprise du développeur courant ; erreur explicite si absente. */
    private Mono<UUID> resoudreOrganisationDuDeveloppeur(BusinessContext ctx) {
        return developerRepository.findByKernelTenantId(ctx.tenantId())
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable.")))
                .flatMap(dev -> dev.getEntrepriseOrganizationId() != null
                        ? Mono.just(dev.getEntrepriseOrganizationId())
                        : Mono.error(ProblemException.unprocessable(
                                "Aucune entreprise associée à votre compte. Choisissez ou créez votre entreprise "
                                        + "(GET/POST /v1/enterprise) avant de créer une application.")
                                .violatedRule("ENTREPRISE_NON_DEFINIE")));
    }

    public Flux<Entreprise> lister(BusinessContext ctx) {
        return depotEntreprise.listerParTenant(ctx.tenantId());
    }

    public Mono<Entreprise> trouver(UUID id, BusinessContext ctx) {
        return depotEntreprise.trouverParId(id)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Application introuvable : " + id)))
                .doOnNext(entreprise -> entreprise.verifierAppartenance(ctx.tenantId()));
    }

    public Mono<Entreprise> changerCycleVie(UUID id, CycleVie cycleVie, BusinessContext ctx) {
        return trouver(id, ctx).flatMap(entreprise -> {
            // Propage la transition au kernel (suspend/close/reopen) avant de persister localement.
            Mono<Void> transitionKernel = entreprise.organizationId() == null
                    ? Mono.empty()
                    : persisterEntreprise.changerCycleVieKernel(entreprise.organizationId(), cycleVie);
            return transitionKernel.then(depotEntreprise.sauvegarder(entreprise.changerCycleVie(cycleVie)))
                    .flatMap(saved -> journaliser(saved, OperationSync.UPDATE));
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
                        "L'application n'a pas d'organisation kernel à approuver."));
            }
            return persisterEntreprise.approuverOrganisation(entreprise.organizationId(), reason)
                    .then(depotEntreprise.sauvegarder(entreprise.changerCycleVie(CycleVie.ACTIVE)))
                    .flatMap(saved -> journaliser(saved, OperationSync.UPDATE));
        });
    }

    /** Met à jour le nom local de l'entreprise (pas de rename kernel). */
    public Mono<Entreprise> modifier(UUID id, String nom, BusinessContext ctx) {
        return trouver(id, ctx)
                .map(entreprise -> entreprise.renommer(nom))
                .flatMap(depotEntreprise::sauvegarder)
                .flatMap(saved -> journaliser(saved, OperationSync.UPDATE));
    }

    /** Archive l'entreprise : cycle de vie {@link CycleVie#FERMEE} (local + kernel {@code close}). */
    public Mono<Void> archiver(UUID id, BusinessContext ctx) {
        return changerCycleVie(id, CycleVie.FERMEE, ctx).then();
    }
}
