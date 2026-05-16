package com.bcaas.core.resource.domain.event;

import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import java.time.Instant;

public sealed interface ResourceDomainEvent
        permits ResourceCreatedEvent, ResourceUpdatedEvent, ResourceSubmittedEvent,
                ResourcePublishedEvent, ResourceRejectedEvent, ResourceArchivedEvent {
    String eventId();
    ResourceId resourceId();
    TenantId tenantId();
    Instant occurredAt();
    String eventType();
}
