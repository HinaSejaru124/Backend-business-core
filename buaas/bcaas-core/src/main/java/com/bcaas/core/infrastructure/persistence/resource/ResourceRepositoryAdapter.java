package com.bcaas.core.infrastructure.persistence.resource;

import com.bcaas.core.resource.domain.model.Resource;
import com.bcaas.core.resource.domain.model.ResourceStatus;
import com.bcaas.core.resource.port.output.ResourceRepository;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ResourceRepositoryAdapter implements ResourceRepository {

    private final ResourceR2dbcRepository r2dbcRepository;

    public ResourceRepositoryAdapter(ResourceR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Resource> save(Resource resource) {
        return r2dbcRepository.save(ResourceEntity.fromDomain(resource))
                .map(ResourceEntity::toDomain);
    }

    @Override
    public Mono<Resource> findById(ResourceId id) {
        return r2dbcRepository.findById(id.value()).map(ResourceEntity::toDomain);
    }

    @Override
    public Flux<Resource> findByTenantId(TenantId tenantId) {
        return r2dbcRepository.findByTenantId(tenantId.value()).map(ResourceEntity::toDomain);
    }

    @Override
    public Flux<Resource> findByOwnerId(ActorId ownerId) {
        return r2dbcRepository.findByOwnerId(ownerId.value()).map(ResourceEntity::toDomain);
    }

    @Override
    public Flux<Resource> findByStatus(TenantId tenantId, ResourceStatus status) {
        return r2dbcRepository.findByTenantIdAndStatus(tenantId.value(), status.name())
                .map(ResourceEntity::toDomain);
    }

    @Override
    public Mono<Long> countByTenantIdAndMonth(TenantId tenantId, int year, int month) {
        return r2dbcRepository.countByTenantIdAndMonth(tenantId.value(), year, month);
    }

    @Override
    public Mono<Void> delete(ResourceId id) {
        return r2dbcRepository.deleteById(id.value());
    }
}
