package com.bcaas.core.workflow.port.output;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.Workflow;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import com.bcaas.core.workflow.domain.model.WorkflowStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WorkflowRepository {
    Mono<Workflow> save(Workflow workflow);
    Mono<Workflow> findById(WorkflowId id);
    Mono<Workflow> findByCorrelationId(String correlationId);
    Flux<Workflow> findByTenantIdAndStatus(TenantId tenantId, WorkflowStatus status);
    Flux<Workflow> findByTenantId(TenantId tenantId);
}
