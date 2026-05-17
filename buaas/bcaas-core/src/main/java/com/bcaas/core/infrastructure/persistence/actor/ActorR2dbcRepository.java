package com.bcaas.core.infrastructure.persistence.actor;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface ActorR2dbcRepository extends R2dbcRepository<ActorEntity, UUID> {
    Mono<ActorEntity> findByEmailAndTenantId(String email, UUID tenantId);
    Flux<ActorEntity> findAllByTenantId(UUID tenantId);
    Mono<Boolean> existsByEmailAndTenantId(String email, UUID tenantId);
    Mono<Long> countByTenantId(UUID tenantId);
}
