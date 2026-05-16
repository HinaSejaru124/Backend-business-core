package com.bcaas.core.actor.domain.event;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;

public sealed interface ActorDomainEvent
        permits ActorCreatedEvent, ActorVerifiedEvent,
                ActorSuspendedEvent, ActorDeactivatedEvent,
                ActorRoleChangedEvent {
    String eventId();
    ActorId actorId();
    TenantId tenantId();
    Instant occurredAt();
    String eventType();
}
