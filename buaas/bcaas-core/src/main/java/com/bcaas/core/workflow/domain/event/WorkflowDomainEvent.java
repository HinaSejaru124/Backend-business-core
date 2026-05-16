package com.bcaas.core.workflow.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import java.time.Instant;

public sealed interface WorkflowDomainEvent
        permits WorkflowCreatedEvent, WorkflowStartedEvent, WorkflowCompletedEvent,
                WorkflowFailedEvent, WorkflowCompensatedEvent {
    String eventId();
    WorkflowId workflowId();
    TenantId tenantId();
    Instant occurredAt();
    String eventType();
}
