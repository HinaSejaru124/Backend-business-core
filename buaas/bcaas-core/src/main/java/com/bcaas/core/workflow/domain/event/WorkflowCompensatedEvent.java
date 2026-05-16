package com.bcaas.core.workflow.domain.event;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import java.time.Instant;
import java.util.UUID;

public record WorkflowCompensatedEvent(
        String eventId, WorkflowId workflowId, TenantId tenantId, Instant occurredAt
) implements WorkflowDomainEvent {
    public WorkflowCompensatedEvent(WorkflowId workflowId, TenantId tenantId) {
        this(UUID.randomUUID().toString(), workflowId, tenantId, Instant.now());
    }
    @Override public String eventType() { return "workflow.compensated"; }
}
