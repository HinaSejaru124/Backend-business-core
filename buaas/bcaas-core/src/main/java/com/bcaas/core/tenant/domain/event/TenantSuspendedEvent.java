package com.bcaas.core.tenant.domain.event;

import com.bcaas.core.shared.domain.TenantId;

import java.time.Instant;
import java.util.UUID;

public record TenantSuspendedEvent(
        String eventId,
        TenantId tenantId,
        String reason,
        Instant occurredAt
) implements TenantDomainEvent {

    public TenantSuspendedEvent(TenantId tenantId, String reason) {
        this(UUID.randomUUID().toString(), tenantId, reason, Instant.now());
    }

    @Override
    public String eventType() { return "tenant.suspended"; }
}
