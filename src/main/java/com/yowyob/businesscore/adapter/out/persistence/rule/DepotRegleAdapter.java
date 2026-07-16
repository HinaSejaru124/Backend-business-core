package com.yowyob.businesscore.adapter.out.persistence.rule;

import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.rule.spi.DepotRegle;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DepotRegleAdapter implements DepotRegle {

    private final RegleMetierRepository repo;

    public DepotRegleAdapter(RegleMetierRepository repo) {
        this.repo = repo;
    }

    @Override
    public Mono<RegleMetier> sauvegarder(RegleMetier regle) {
        return repo.existsById(regle.getId()).flatMap(existe -> {
            RegleMetierEntity e = toEntity(regle);
            if (existe) {
                return repo.findById(regle.getId()).flatMap(existant -> {
                    e.setCreatedAt(existant.getCreatedAt());
                    e.setUpdatedAt(OffsetDateTime.now());
                    e.enModification();
                    return repo.save(e).map(DepotRegleAdapter::versDomaine);
                });
            }
            return repo.save(e).map(DepotRegleAdapter::versDomaine);
        });
    }

    @Override
    public Mono<RegleMetier> trouverParId(UUID id) {
        return repo.findById(id).map(DepotRegleAdapter::versDomaine);
    }

    @Override
    public Flux<RegleMetier> listerParVersionType(UUID versionTypeId) {
        return repo.findByVersionTypeId(versionTypeId).map(DepotRegleAdapter::versDomaine);
    }

    @Override
    public Flux<RegleMetier> listerParEntreprise(UUID entrepriseId) {
        return repo.findByEntrepriseId(entrepriseId).map(DepotRegleAdapter::versDomaine);
    }

    @Override
    public Mono<Void> supprimer(UUID id) {
        return repo.deleteById(id);
    }

    private static RegleMetierEntity toEntity(RegleMetier r) {
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

    static RegleMetier versDomaine(RegleMetierEntity e) {
        return new RegleMetier(
                e.getId(), e.getTenantId(),
                e.getVersionTypeId(), e.getEntrepriseId(),
                Declencheur.valueOf(e.getDeclencheur()),
                e.getCondition(),
                Effet.valueOf(e.getEffet()),
                e.getRolesAutorisesADeroger());
    }
}
