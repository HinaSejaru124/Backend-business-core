package com.bcaas.core.actor.domain.event;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ActorSuspendedEvent(
        String eventId, ActorId actorId, TenantId tenantId,
        String reason, Instant occurredAt
) implements ActorDomainEvent {
    public ActorSuspendedEvent(ActorId actorId, TenantId tenantId, String reason) {
        this(UUID.randomUUID().toString(), actorId, tenantId, reason, Instant.now());
    }
    @Override public String eventType() { return "actor.suspended"; }
}
