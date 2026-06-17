package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/** Repository des versions de type métier. RLS garantit l'isolation tenant. */
public interface VersionTypeRepository
        extends ReactiveCrudRepository<VersionTypeEntity, UUID> {

    Flux<VersionTypeEntity> findAllByTypeMetierId(UUID typeMetierId);
}
