package com.yowyob.businesscore.application.usecase.rule;

import com.yowyob.businesscore.adapter.out.persistence.businesstype.VersionTypeRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync.OperationSync;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync.TypeEntiteSync;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.rule.spi.DepotRegle;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * CRUD des règles métier (Type ou locales). Le {@code tenantId} vient toujours du
 * {@link BusinessContext}. Les règles locales (rattachées à une entreprise) sont journalisées pour la
 * synchronisation pull des backends terminaux ; les règles de Type (partagées entre entreprises via une
 * version) ne le sont pas — hors périmètre d'un journal par entreprise.
 */
@Service
public class GestionRegleService {

    private final DepotRegle depot;
    private final VersionTypeRepository versionTypeRepo;
    private final LireEntreprise lireEntreprise;
    private final JournaliserChangementSync journaliserSync;

    public GestionRegleService(DepotRegle depot, VersionTypeRepository versionTypeRepo,
                               LireEntreprise lireEntreprise, JournaliserChangementSync journaliserSync) {
        this.depot = depot;
        this.versionTypeRepo = versionTypeRepo;
        this.lireEntreprise = lireEntreprise;
        this.journaliserSync = journaliserSync;
    }

    private Mono<RegleMetier> journaliser(RegleMetier regle, OperationSync operation) {
        return journaliserSync.journaliser(regle.getTenantId(), regle.getEntrepriseId(), TypeEntiteSync.RULE,
                        regle.getId(), operation, regle)
                .thenReturn(regle);
    }

    public Mono<RegleMetier> creerRegleDeType(
            UUID typeId, int numeroVersion, Declencheur declencheur, String condition,
            Effet effet, List<String> rolesAutorisesADeroger, BusinessContext ctx) {
        return versionTypeRepo.findByTypeMetierIdAndNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMap(version -> depot.sauvegarder(new RegleMetier(
                        UUID.randomUUID(), ctx.tenantId(),
                        version.getId(), null,
                        declencheur, condition, effet, rolesAutorisesADeroger)));
    }

    public Mono<RegleMetier> creerRegleLocale(
            UUID entrepriseId, Declencheur declencheur, String condition,
            Effet effet, List<String> rolesAutorisesADeroger, BusinessContext ctx) {
        return lireEntreprise.parId(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMap(entreprise -> depot.sauvegarder(new RegleMetier(
                        UUID.randomUUID(), ctx.tenantId(),
                        null, entrepriseId,
                        declencheur, condition, effet, rolesAutorisesADeroger)))
                .flatMap(regle -> journaliser(regle, OperationSync.CREATE));
    }

    public Flux<RegleMetier> listerParVersion(UUID typeId, int numeroVersion) {
        return versionTypeRepo.findByTypeMetierIdAndNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMapMany(version -> depot.listerParVersionType(version.getId()));
    }

    public Flux<RegleMetier> listerParEntreprise(UUID entrepriseId) {
        return lireEntreprise.parId(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMapMany(e -> depot.listerParEntreprise(entrepriseId));
    }

    public Mono<RegleMetier> trouverDeType(UUID typeId, int numeroVersion, UUID ruleId) {
        return versionTypeRepo.findByTypeMetierIdAndNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMap(version -> depot.trouverParId(ruleId)
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Règle introuvable : " + ruleId)))
                        .filter(r -> version.getId().equals(r.getVersionTypeId()))
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Règle " + ruleId + " absente de cette version"))));
    }

    public Mono<RegleMetier> trouverLocale(UUID entrepriseId, UUID ruleId) {
        return depot.trouverParId(ruleId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Règle introuvable : " + ruleId)))
                .filter(r -> entrepriseId.equals(r.getEntrepriseId()))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Règle " + ruleId + " absente de cette entreprise")));
    }

    public Mono<RegleMetier> modifierDeType(
            UUID typeId, int numeroVersion, UUID ruleId, Declencheur declencheur,
            String condition, Effet effet, List<String> rolesAutorisesADeroger) {
        return trouverDeType(typeId, numeroVersion, ruleId)
                .flatMap(existante -> depot.sauvegarder(new RegleMetier(
                        existante.getId(), existante.getTenantId(),
                        existante.getVersionTypeId(), null,
                        declencheur, condition, effet, rolesAutorisesADeroger)));
    }

    public Mono<RegleMetier> modifierLocale(
            UUID entrepriseId, UUID ruleId, Declencheur declencheur,
            String condition, Effet effet, List<String> rolesAutorisesADeroger) {
        return trouverLocale(entrepriseId, ruleId)
                .flatMap(existante -> depot.sauvegarder(new RegleMetier(
                        existante.getId(), existante.getTenantId(),
                        null, existante.getEntrepriseId(),
                        declencheur, condition, effet, rolesAutorisesADeroger)))
                .flatMap(regle -> journaliser(regle, OperationSync.UPDATE));
    }

    public Mono<Void> supprimerDeType(UUID typeId, int numeroVersion, UUID ruleId) {
        return trouverDeType(typeId, numeroVersion, ruleId)
                .flatMap(r -> depot.supprimer(r.getId()));
    }

    public Mono<Void> supprimerLocale(UUID entrepriseId, UUID ruleId) {
        return trouverLocale(entrepriseId, ruleId)
                .flatMap(r -> depot.supprimer(r.getId()).thenReturn(r))
                .flatMap(regle -> journaliser(regle, OperationSync.DELETE))
                .then();
    }
}
