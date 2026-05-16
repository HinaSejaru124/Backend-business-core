package com.bcaas.core.resource.port.output;

import com.bcaas.core.resource.domain.model.Resource;
import com.bcaas.core.resource.domain.model.ResourceStatus;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResourceRepository {
    Mono<Resource> save(Resource resource);
    Mono<Resource> findById(ResourceId id);
    Flux<Resource> findByTenantId(TenantId tenantId);
    Flux<Resource> findByOwnerId(ActorId ownerId);
    Flux<Resource> findByStatus(TenantId tenantId, ResourceStatus status);
    Mono<Long> countByTenantIdAndMonth(TenantId tenantId, int year, int month);
    Mono<Void> delete(ResourceId id);
}
