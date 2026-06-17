package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adapter R2DBC — implémente PersisterVersionType. */
@Component
public class VersionTypePersistenceAdapter implements PersisterVersionType {

    private final VersionTypeRepository repository;

    public VersionTypePersistenceAdapter(VersionTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<VersionType> sauvegarder(VersionType version) {
        return repository.save(versEntity(version)).map(this::versDomaine);
    }

    @Override
    public Mono<VersionType> trouverParId(UUID id) {
        return repository.findById(id).map(this::versDomaine);
    }

    @Override
    public Flux<VersionType> listerParTypeMetier(UUID typeMetierId) {
        return repository.findAllByTypeMetierId(typeMetierId).map(this::versDomaine);
    }

    @Override
    public Mono<Integer> dernierNumero(UUID typeMetierId) {
        return repository.findAllByTypeMetierId(typeMetierId)
                .map(VersionTypeEntity::getNumero)
                .reduce(0, Integer::max)
                .defaultIfEmpty(0);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private VersionTypeEntity versEntity(VersionType v) {
        return VersionTypeEntity.nouveau(
                v.id(), v.tenantId(), v.typeMetierId(),
                v.numero(), v.immuable(), v.publieeLe()
        );
    }

    private VersionType versDomaine(VersionTypeEntity e) {
        return new VersionType(
                e.getId(), e.getTenantId(), e.getTypeMetierId(),
                e.getNumero(), e.isImmuable(), e.getPublieeLe()
        );
    }
}
