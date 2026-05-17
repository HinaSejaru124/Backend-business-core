package com.bcaas.core.infrastructure.persistence.actor;

import com.bcaas.core.actor.domain.model.Actor;
import com.bcaas.core.actor.port.output.ActorRepository;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ActorRepositoryAdapter implements ActorRepository {

    private final ActorR2dbcRepository r2dbcRepository;

    public ActorRepositoryAdapter(ActorR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Actor> save(Actor actor) {
        return r2dbcRepository.save(ActorEntity.fromDomain(actor))
                .map(ActorEntity::toDomain);
    }

    @Override
    public Mono<Actor> findById(ActorId id) {
        return r2dbcRepository.findById(id.value()).map(ActorEntity::toDomain);
    }

    @Override
    public Mono<Actor> findByEmailAndTenantId(String email, TenantId tenantId) {
        return r2dbcRepository.findByEmailAndTenantId(email, tenantId.value())
                .map(ActorEntity::toDomain);
    }

    @Override
    public Flux<Actor> findAllByTenantId(TenantId tenantId) {
        return r2dbcRepository.findAllByTenantId(tenantId.value())
                .map(ActorEntity::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmailAndTenantId(String email, TenantId tenantId) {
        return r2dbcRepository.existsByEmailAndTenantId(email, tenantId.value());
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return r2dbcRepository.countByTenantId(tenantId.value());
    }

    @Override
    public Mono<Void> delete(ActorId id) {
        return r2dbcRepository.deleteById(id.value());
    }
}
