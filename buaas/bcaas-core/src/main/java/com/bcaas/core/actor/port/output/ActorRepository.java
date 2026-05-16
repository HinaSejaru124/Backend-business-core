package com.bcaas.core.actor.port.output;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.actor.domain.model.Actor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ActorRepository {
    Mono<Actor> save(Actor actor);
    Mono<Actor> findById(ActorId id);
    Mono<Actor> findByEmailAndTenantId(String email, TenantId tenantId);
    Flux<Actor> findAllByTenantId(TenantId tenantId);
    Mono<Boolean> existsByEmailAndTenantId(String email, TenantId tenantId);
    Mono<Long> countByTenantId(TenantId tenantId);
    Mono<Void> delete(ActorId id);
}
