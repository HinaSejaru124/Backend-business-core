package com.bcaas.core.resource.domain.event;

import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ResourceUpdatedEvent(
        String eventId, ResourceId resourceId, TenantId tenantId, Instant occurredAt
) implements ResourceDomainEvent {
    public ResourceUpdatedEvent(ResourceId resourceId, TenantId tenantId) {
        this(UUID.randomUUID().toString(), resourceId, tenantId, Instant.now());
    }
    @Override public String eventType() { return "resource.updated"; }
}
