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
        // INSERT à la création, UPDATE à la publication (même id) : on recharge la ligne
        // existante (isNew()=false → UPDATE) ; absente → fabrique d'insertion.
        return repository.findById(version.id())
                .map(existant -> appliquer(existant, version))
                .switchIfEmpty(Mono.fromSupplier(() -> versEntityNouveau(version)))
                .flatMap(repository::save)
                .map(this::versDomaine);
    }

    @Override
    public Mono<VersionType> trouverParId(UUID id) {
        return repository.findById(id).map(this::versDomaine);
    }

    @Override
    public Mono<VersionType> trouverParTypeEtNumero(UUID typeMetierId, int numero) {
        return repository.findByTypeMetierIdAndNumero(typeMetierId, numero).map(this::versDomaine);
    }

    @Override
    public Flux<VersionType> listerParTypeMetier(UUID typeMetierId) {
        return repository.findAllByTypeMetierId(typeMetierId).map(this::versDomaine);
    }

    @Override
    public Mono<Integer> dernierNumero(UUID typeMetierId) {
        // MAX(numero) en une requête plutôt que de streamer toutes les versions.
        return repository.findMaxNumero(typeMetierId).defaultIfEmpty(0);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private VersionTypeEntity versEntityNouveau(VersionType v) {
        return VersionTypeEntity.nouveau(
                v.id(), v.tenantId(), v.typeMetierId(),
                v.numero(), v.immuable(), v.publieeLe()
        );
    }

    /** Applique les champs modifiables sur une ligne existante (isNew()=false → UPDATE). */
    private VersionTypeEntity appliquer(VersionTypeEntity e, VersionType v) {
        e.setNumero(v.numero());
        e.setImmuable(v.immuable());
        e.setPublieeLe(v.publieeLe());
        return e;
    }

    private VersionType versDomaine(VersionTypeEntity e) {
        return new VersionType(
                e.getId(), e.getTenantId(), e.getTypeMetierId(),
                e.getNumero(), e.isImmuable(), e.getPublieeLe()
        );
    }
}
