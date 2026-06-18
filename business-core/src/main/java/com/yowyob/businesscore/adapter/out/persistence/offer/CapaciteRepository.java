package com.yowyob.businesscore.adapter.out.persistence.offer;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface CapaciteRepository extends ReactiveCrudRepository<CapaciteEntity, UUID> {
    Flux<CapaciteEntity> findByDefinitionOffreId(UUID definitionOffreId);
}