// application/usecase/rule/CreerRegleUseCase.java
package com.yowyob.businesscore.application.usecase.rule;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.yowyob.businesscore.adapter.out.persistence.rule.RegleMetierEntity;
import com.yowyob.businesscore.adapter.out.persistence.rule.RegleMetierRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import reactor.core.publisher.Mono;

@Component
public class CreerRegleUseCase {

    private final RegleMetierRepository repo;

    public CreerRegleUseCase(RegleMetierRepository repo) {
        this.repo = repo;
    }

    public Mono<RegleMetier> creerRegleDeType(
            UUID versionTypeId,
            Declencheur declencheur,
            String condition,
            Effet effet,
            List<String> rolesAutorisesADeroger,
            BusinessContext ctx) {

        UUID tenantId = ctx.tenantId();

        RegleMetier regle = new RegleMetier(
                UUID.randomUUID(), tenantId,
                versionTypeId, null,
                declencheur, condition,
                effet, rolesAutorisesADeroger);

        return sauvegarder(regle);
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