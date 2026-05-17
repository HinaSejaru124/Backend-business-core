package com.bcaas.core.infrastructure.persistence.tenant;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * Repository Spring Data R2DBC pour TenantEntity.
 * Interface technique — ne sort jamais de la couche infrastructure.
 */
@Repository
public interface TenantR2dbcRepository extends R2dbcRepository<TenantEntity, UUID> {
    Mono<TenantEntity> findBySlug(String slug);
    Mono<Boolean> existsBySlug(String slug);
}
