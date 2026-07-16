package com.yowyob.businesscore.adapter.out.persistence.offer;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface DefinitionOffreRepository extends ReactiveCrudRepository<DefinitionOffreEntity, UUID> {
    Flux<DefinitionOffreEntity> findByVersionTypeId(UUID versionTypeId);
}