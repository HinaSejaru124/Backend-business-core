package com.bcaas.core.infrastructure.persistence.resource;

import com.bcaas.core.resource.domain.model.ResourceStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Repository
public interface ResourceR2dbcRepository extends R2dbcRepository<ResourceEntity, UUID> {
    Flux<ResourceEntity> findByTenantId(UUID tenantId);
    Flux<ResourceEntity> findByOwnerId(UUID ownerId);
    Flux<ResourceEntity> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("""
        SELECT COUNT(*) FROM bcaas_resources
        WHERE tenant_id = :tenantId
        AND EXTRACT(YEAR FROM created_at) = :year
        AND EXTRACT(MONTH FROM created_at) = :month
    """)
    Mono<Long> countByTenantIdAndMonth(UUID tenantId, int year, int month);
}
