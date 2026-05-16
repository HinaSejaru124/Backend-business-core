package com.bcaas.core.actor.domain.event;

import com.bcaas.core.actor.domain.model.ActorRole;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ActorRoleChangedEvent(
        String eventId, ActorId actorId, TenantId tenantId,
        ActorRole oldRole, ActorRole newRole, Instant occurredAt
) implements ActorDomainEvent {
    public ActorRoleChangedEvent(ActorId actorId, TenantId tenantId,
                                  ActorRole oldRole, ActorRole newRole) {
        this(UUID.randomUUID().toString(), actorId, tenantId, oldRole, newRole, Instant.now());
    }
    @Override public String eventType() { return "actor.role_changed"; }
}
