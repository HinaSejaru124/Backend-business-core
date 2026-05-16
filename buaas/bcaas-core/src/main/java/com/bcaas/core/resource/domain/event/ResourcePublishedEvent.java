package com.bcaas.core.resource.domain.event;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ResourcePublishedEvent(
        String eventId, ResourceId resourceId, TenantId tenantId,
        ActorId ownerId, Instant occurredAt
) implements ResourceDomainEvent {
    public ResourcePublishedEvent(ResourceId resourceId, TenantId tenantId, ActorId ownerId) {
        this(UUID.randomUUID().toString(), resourceId, tenantId, ownerId, Instant.now());
    }
    @Override public String eventType() { return "resource.published"; }
}
