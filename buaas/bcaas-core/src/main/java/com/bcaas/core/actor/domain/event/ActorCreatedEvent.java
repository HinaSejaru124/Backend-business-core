package com.bcaas.core.actor.domain.event;

import com.bcaas.core.actor.domain.model.ActorRole;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ActorCreatedEvent(
        String eventId, ActorId actorId, TenantId tenantId,
        String email, ActorRole role, Instant occurredAt
) implements ActorDomainEvent {
    public ActorCreatedEvent(ActorId actorId, TenantId tenantId, String email, ActorRole role) {
        this(UUID.randomUUID().toString(), actorId, tenantId, email, role, Instant.now());
    }
    @Override public String eventType() { return "actor.created"; }
}
