package com.bcaas.core.tenant.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.TenantPlan;

import java.time.Instant;
import java.util.UUID;

public record TenantCreatedEvent(
        String eventId,
        TenantId tenantId,
        String name,
        String slug,
        TenantPlan plan,
        Instant occurredAt
) implements TenantDomainEvent {

    public TenantCreatedEvent(TenantId tenantId, String name, String slug, TenantPlan plan) {
        this(UUID.randomUUID().toString(), tenantId, name, slug, plan, Instant.now());
    }

    @Override
    public String eventType() { return "tenant.created"; }
}
