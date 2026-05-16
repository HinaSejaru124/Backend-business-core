package com.bcaas.core.actor.port.input;

import com.bcaas.core.actor.domain.model.Actor;
import com.bcaas.core.actor.domain.model.ActorIdentity;
import com.bcaas.core.actor.domain.model.ActorProfile;
import com.bcaas.core.actor.domain.model.ActorRole;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ActorUseCase {
    Mono<Actor> createActor(TenantId tenantId, ActorIdentity identity,
                            ActorRole role, BusinessContext context);
    Mono<Actor> verifyActor(ActorId actorId, BusinessContext context);
    Mono<Actor> suspendActor(ActorId actorId, String reason, BusinessContext context);
    Mono<Actor> reactivateActor(ActorId actorId, BusinessContext context);
    Mono<Actor> updateProfile(ActorId actorId, ActorProfile profile, BusinessContext context);
    Mono<Actor> changeRole(ActorId actorId, ActorRole newRole, BusinessContext context);
    Mono<Actor> findById(ActorId actorId);
    Flux<Actor> findAllByTenant(TenantId tenantId);
}
