package com.yowyob.businesscore.adapter.out.persistence.trace;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository des traces d'opération. RLS garantit l'isolation ; {@code cle_idempotence} est unique
 * par tenant (un rejeu retrouve la trace existante au lieu de créer un doublon).
 */
public interface TraceOperationRepository
        extends ReactiveCrudRepository<TraceOperationEntity, UUID> {

    Mono<TraceOperationEntity> findByCleIdempotence(String cleIdempotence);

    Flux<TraceOperationEntity> findByEntrepriseId(UUID entrepriseId);
}
