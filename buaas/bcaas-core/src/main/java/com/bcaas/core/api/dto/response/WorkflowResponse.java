package com.bcaas.core.api.dto.response;

import com.bcaas.core.workflow.domain.model.Workflow;
import java.time.Instant;

public record WorkflowResponse(
        String id,
        String tenantId,
        String workflowType,
        String correlationId,
        String status,
        int progressPercent,
        String currentStep,
        Instant startedAt,
        Instant completedAt
) {
    public static WorkflowResponse from(Workflow workflow) {
        String currentStep = null;
        try {
            currentStep = workflow.isRunning()
                    ? workflow.getCurrentStep().getName() : null;
        } catch (Exception ignored) {}

        return new WorkflowResponse(
                workflow.getId().toString(),
                workflow.getTenantId().toString(),
                workflow.getWorkflowType(),
                workflow.getCorrelationId(),
                workflow.getStatus().name(),
                workflow.getProgressPercent(),
                currentStep,
                workflow.getStartedAt(),
                workflow.getCompletedAt()
        );
    }
}
