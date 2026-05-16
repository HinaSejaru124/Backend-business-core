package com.bcaas.core.tenant.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;
import java.util.UUID;

/**
 * Interface de base pour tous les événements domaine du Tenant.
 * Analogie réseau : paquet émis après un changement d'état.
 */
public sealed interface TenantDomainEvent
        permits TenantCreatedEvent, TenantActivatedEvent,
                TenantSuspendedEvent, TenantDeactivatedEvent {

    String eventId();
    TenantId tenantId();
    Instant occurredAt();
    String eventType();
}
