package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import com.yowyob.businesscore.domain.configuration.ParametreConfig;
import com.yowyob.businesscore.domain.port.out.PersisterParametreConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adapter R2DBC — implémente PersisterParametreConfig. */
@Component
public class ParametreConfigPersistenceAdapter implements PersisterParametreConfig {

    private final ParametreConfigRepository repository;

    public ParametreConfigPersistenceAdapter(ParametreConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<ParametreConfig> sauvegarder(ParametreConfig p) {
        return repository.save(versEntity(p)).map(this::versDomaine);
    }

    @Override
    public Mono<ParametreConfig> trouverParCleEtVersion(String cle, UUID versionTypeId) {
        return repository.findByCleAndVersionTypeId(cle, versionTypeId).map(this::versDomaine);
    }

    @Override
    public Mono<ParametreConfig> trouverParCleEtEntreprise(String cle, UUID entrepriseId) {
        return repository.findByCleAndEntrepriseId(cle, entrepriseId).map(this::versDomaine);
    }

    @Override
    public Flux<ParametreConfig> listerParVersion(UUID versionTypeId) {
        return repository.findAllByVersionTypeId(versionTypeId).map(this::versDomaine);
    }

    @Override
    public Flux<ParametreConfig> listerParEntreprise(UUID entrepriseId) {
        return repository.findAllByEntrepriseId(entrepriseId).map(this::versDomaine);
    }

    @Override
    public Mono<Void> supprimer(UUID id) {
        return repository.deleteById(id);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private ParametreConfigEntity versEntity(ParametreConfig p) {
        return ParametreConfigEntity.nouveau(
                p.id(), p.tenantId(), p.versionTypeId(),
                p.entrepriseId(), p.cle(), p.valeur(), p.verrouille()
        );
    }

    private ParametreConfig versDomaine(ParametreConfigEntity e) {
        return new ParametreConfig(
                e.getId(), e.getTenantId(), e.getVersionTypeId(),
                e.getEntrepriseId(), e.getCle(), e.getValeur(), e.isVerrouille()
        );
    }
}
