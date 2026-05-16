package com.bcaas.core.workflow.port.input;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.Workflow;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import com.bcaas.core.workflow.domain.model.WorkflowStep;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

public interface WorkflowUseCase {
    Mono<Workflow> createAndStart(String workflowType, String correlationId,
                                  List<WorkflowStep> steps, Map<String, String> context,
                                  BusinessContext businessContext);
    Mono<Workflow> completeStep(WorkflowId workflowId, Map<String, String> outputData,
                                BusinessContext context);
    Mono<Workflow> failStep(WorkflowId workflowId, String errorMessage,
                            BusinessContext context);
    Mono<Workflow> compensate(WorkflowId workflowId, BusinessContext context);
    Mono<Workflow> findById(WorkflowId workflowId);
    Flux<Workflow> findRunningByTenant(TenantId tenantId);
}
