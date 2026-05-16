package com.bcaas.core.resource.domain.event;

import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

public record ResourceRejectedEvent(
        String eventId, ResourceId resourceId, TenantId tenantId,
        String reason, Instant occurredAt
) implements ResourceDomainEvent {
    public ResourceRejectedEvent(ResourceId resourceId, TenantId tenantId, String reason) {
        this(UUID.randomUUID().toString(), resourceId, tenantId, reason, Instant.now());
    }
    @Override public String eventType() { return "resource.rejected"; }
}
