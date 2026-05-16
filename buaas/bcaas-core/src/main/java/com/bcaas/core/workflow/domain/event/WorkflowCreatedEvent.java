package com.bcaas.core.workflow.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import java.time.Instant;
import java.util.UUID;

public record WorkflowCreatedEvent(
        String eventId, WorkflowId workflowId, TenantId tenantId,
        String workflowType, String correlationId, Instant occurredAt
) implements WorkflowDomainEvent {
    public WorkflowCreatedEvent(WorkflowId workflowId, TenantId tenantId,
                                 String workflowType, String correlationId) {
        this(UUID.randomUUID().toString(), workflowId, tenantId,
             workflowType, correlationId, Instant.now());
    }
    @Override public String eventType() { return "workflow.created"; }
}
