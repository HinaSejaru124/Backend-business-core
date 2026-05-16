package com.bcaas.core.actor.domain.event;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ActorDeactivatedEvent(
        String eventId, ActorId actorId, TenantId tenantId, Instant occurredAt
) implements ActorDomainEvent {
    public ActorDeactivatedEvent(ActorId actorId, TenantId tenantId) {
        this(UUID.randomUUID().toString(), actorId, tenantId, Instant.now());
    }
    @Override public String eventType() { return "actor.deactivated"; }
}
