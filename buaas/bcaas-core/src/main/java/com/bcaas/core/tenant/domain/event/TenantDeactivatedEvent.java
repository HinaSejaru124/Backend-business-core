package com.bcaas.core.tenant.domain.event;

import com.bcaas.core.shared.domain.TenantId;

import java.time.Instant;
import java.util.UUID;

public record TenantDeactivatedEvent(
        String eventId,
        TenantId tenantId,
        Instant occurredAt
) implements TenantDomainEvent {

    public TenantDeactivatedEvent(TenantId tenantId) {
        this(UUID.randomUUID().toString(), tenantId, Instant.now());
    }

    @Override
    public String eventType() { return "tenant.deactivated"; }
}
