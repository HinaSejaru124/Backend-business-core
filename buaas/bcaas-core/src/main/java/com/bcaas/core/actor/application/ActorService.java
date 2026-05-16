package com.bcaas.core.actor.application;

import com.bcaas.core.actor.domain.model.*;
import com.bcaas.core.actor.port.input.ActorUseCase;
import com.bcaas.core.actor.port.output.ActorEventPublisher;
import com.bcaas.core.actor.port.output.ActorRepository;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.port.output.TenantRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ActorService implements ActorUseCase {

    private final ActorRepository actorRepository;
    private final ActorEventPublisher eventPublisher;
    private final TenantRepository tenantRepository;

    public ActorService(ActorRepository actorRepository,
                        ActorEventPublisher eventPublisher,
                        TenantRepository tenantRepository) {
        this.actorRepository = actorRepository;
        this.eventPublisher = eventPublisher;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Mono<Actor> createActor(TenantId tenantId, ActorIdentity identity,
                                   ActorRole role, BusinessContext context) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable : " + tenantId)))
                .flatMap(tenant -> actorRepository.countByTenantId(tenantId)
                        .flatMap(count -> {
                            if (!tenant.canCreateActor(count.intValue())) {
                                return Mono.error(new IllegalStateException(
                                    "Limite d'acteurs atteinte pour le plan " + tenant.getPlan()));
                            }
                            return actorRepository.existsByEmailAndTenantId(
                                    identity.email(), tenantId);
                        })
                        .flatMap(emailExists -> {
                            if (emailExists) {
                                return Mono.error(new IllegalArgumentException(
                                    "Email déjà utilisé dans ce tenant : " + identity.email()));
                            }
                            Actor actor = Actor.create(tenantId, identity, role, context.actorId());
                            return actorRepository.save(actor)
                                    .flatMap(this::publishEventsAndReturn);
                        }));
    }

    @Override
    public Mono<Actor> verifyActor(ActorId actorId, BusinessContext context) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)))
                .flatMap(actor -> {
                    actor.verify(context.actorId());
                    return actorRepository.save(actor)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Actor> suspendActor(ActorId actorId, String reason, BusinessContext context) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)))
                .flatMap(actor -> {
                    actor.suspend(context.actorId(), reason);
                    return actorRepository.save(actor)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Actor> reactivateActor(ActorId actorId, BusinessContext context) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)))
                .flatMap(actor -> {
                    actor.reactivate(context.actorId());
                    return actorRepository.save(actor);
                });
    }

    @Override
    public Mono<Actor> updateProfile(ActorId actorId, ActorProfile profile,
                                     BusinessContext context) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)))
                .flatMap(actor -> {
                    actor.updateProfile(profile, context.actorId());
                    return actorRepository.save(actor);
                });
    }

    @Override
    public Mono<Actor> changeRole(ActorId actorId, ActorRole newRole, BusinessContext context) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)))
                .flatMap(actor -> {
                    actor.changeRole(newRole, context.actorId());
                    return actorRepository.save(actor)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Actor> findById(ActorId actorId) {
        return actorRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Acteur introuvable : " + actorId)));
    }

    @Override
    public Flux<Actor> findAllByTenant(TenantId tenantId) {
        return actorRepository.findAllByTenantId(tenantId);
    }

    private Mono<Actor> publishEventsAndReturn(Actor actor) {
        return Mono.when(
                actor.getDomainEvents().stream()
                        .map(eventPublisher::publish)
                        .toList()
        ).doOnSuccess(v -> actor.clearDomainEvents())
         .thenReturn(actor);
    }
}
