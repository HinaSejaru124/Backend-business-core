package com.bcaas.core.workflow.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import java.time.Instant;
import java.util.UUID;

public record WorkflowCompletedEvent(
        String eventId, WorkflowId workflowId, TenantId tenantId,
        String workflowType, Instant occurredAt
) implements WorkflowDomainEvent {
    public WorkflowCompletedEvent(WorkflowId workflowId, TenantId tenantId, String workflowType) {
        this(UUID.randomUUID().toString(), workflowId, tenantId, workflowType, Instant.now());
    }
    @Override public String eventType() { return "workflow.completed"; }
}
