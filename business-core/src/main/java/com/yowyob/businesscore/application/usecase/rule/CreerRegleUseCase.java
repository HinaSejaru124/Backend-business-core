// application/usecase/rule/CreerRegleUseCase.java
package com.yowyob.businesscore.application.usecase.rule;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.businesscore.adapter.out.persistence.businesstype.VersionTypeRepository;
import com.yowyob.businesscore.adapter.out.persistence.rule.RegleMetierEntity;
import com.yowyob.businesscore.adapter.out.persistence.rule.RegleMetierRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import reactor.core.publisher.Mono;

/**
 * Use case de création d'une règle, de Type ou locale.
 *
 * <p>Le {@code tenantId} est toujours lu du {@link BusinessContext} (jamais d'un payload client) :
 * c'est la frontière d'isolation, garantie en plus par la RLS à l'écriture. La règle de domaine
 * {@link RegleMetier} valide l'invariant de portée avant la persistance.
 */
@Component
public class CreerRegleUseCase {

    private final RegleMetierRepository repo;
    private final VersionTypeRepository versionTypeRepo;

    public CreerRegleUseCase(RegleMetierRepository repo, VersionTypeRepository versionTypeRepo) {
        this.repo = repo;
        this.versionTypeRepo = versionTypeRepo;
    }

    /**
     * Crée une règle de Type. La version cible est identifiée par l'URL ({@code typeId} + {@code numeroVersion}),
     * conformément au contrat OpenAPI ; on la résout via la table socle {@code version_type} (isolée par la
     * RLS), ce qui revérifie au passage qu'elle appartient bien au tenant courant. Renvoie 404 si la version
     * n'existe pas pour ce tenant.
     */
    public Mono<RegleMetier> creerRegleDeType(
            UUID typeId,
            int numeroVersion,
            Declencheur declencheur,
            String condition,
            Effet effet,
            List<String> rolesAutorisesADeroger,
            BusinessContext ctx) {

        UUID tenantId = ctx.tenantId();

        return versionTypeRepo.findByTypeMetierIdAndNumero(typeId, numeroVersion)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Version " + numeroVersion + " introuvable pour le type " + typeId)))
                .flatMap(version -> {
                    RegleMetier regle = new RegleMetier(
                            UUID.randomUUID(), tenantId,
                            version.getId(), null,
                            declencheur, condition,
                            effet, rolesAutorisesADeroger);
                    return sauvegarder(regle);
                });
    }

    public Mono<RegleMetier> creerRegleLocale(
            UUID entrepriseId,
            Declencheur declencheur,
            String condition,
            Effet effet,
            List<String> rolesAutorisesADeroger,
            BusinessContext ctx) {

        UUID tenantId = ctx.tenantId();

        RegleMetier regle = new RegleMetier(
                UUID.randomUUID(), tenantId,
                null, entrepriseId,
                declencheur, condition,
                effet, rolesAutorisesADeroger);

        return sauvegarder(regle);
    }

    private Mono<RegleMetier> sauvegarder(RegleMetier regle) {
        return repo.save(toEntity(regle)).thenReturn(regle);
    }

    private RegleMetierEntity toEntity(RegleMetier r) {
        RegleMetierEntity e = new RegleMetierEntity();
        e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setVersionTypeId(r.getVersionTypeId());
        e.setEntrepriseId(r.getEntrepriseId());
        e.setDeclencheur(r.getDeclencheur().name());
        e.setCondition(r.getCondition());
        e.setEffet(r.getEffet().name());
        e.setRolesAutorisesADeroger(r.getRolesAutorisesADeroger());
        e.setCreatedAt(OffsetDateTime.now());
        e.setUpdatedAt(OffsetDateTime.now());
        return e;
    }
}