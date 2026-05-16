package com.bcaas.core.workflow.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import java.time.Instant;
import java.util.UUID;

public record WorkflowFailedEvent(
        String eventId, WorkflowId workflowId, TenantId tenantId,
        String failedStep, String errorMessage, Instant occurredAt
) implements WorkflowDomainEvent {
    public WorkflowFailedEvent(WorkflowId workflowId, TenantId tenantId,
                                String failedStep, String errorMessage) {
        this(UUID.randomUUID().toString(), workflowId, tenantId,
             failedStep, errorMessage, Instant.now());
    }
    @Override public String eventType() { return "workflow.failed"; }
}
