package com.bcaas.core.resource.domain.event;

import com.bcaas.core.resource.domain.model.ResourceType;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ResourceCreatedEvent(
        String eventId, ResourceId resourceId, TenantId tenantId,
        ActorId ownerId, ResourceType type, Instant occurredAt
) implements ResourceDomainEvent {
    public ResourceCreatedEvent(ResourceId resourceId, TenantId tenantId,
                                 ActorId ownerId, ResourceType type) {
        this(UUID.randomUUID().toString(), resourceId, tenantId, ownerId, type, Instant.now());
    }
    @Override public String eventType() { return "resource.created"; }
}
