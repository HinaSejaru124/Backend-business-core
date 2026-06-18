package com.yowyob.businesscore.adapter.out.persistence.actor;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface ActeurMetierRepository extends ReactiveCrudRepository<ActeurMetierEntity, UUID> {
    Flux<ActeurMetierEntity> findByEntrepriseId(UUID entrepriseId);
}