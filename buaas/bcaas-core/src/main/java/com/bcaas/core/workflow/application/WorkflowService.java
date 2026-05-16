package com.bcaas.core.workflow.application;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.model.*;
import com.bcaas.core.workflow.port.input.WorkflowUseCase;
import com.bcaas.core.workflow.port.output.WorkflowEventPublisher;
import com.bcaas.core.workflow.port.output.WorkflowRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

public class WorkflowService implements WorkflowUseCase {

    private final WorkflowRepository workflowRepository;
    private final WorkflowEventPublisher eventPublisher;

    public WorkflowService(WorkflowRepository workflowRepository,
                           WorkflowEventPublisher eventPublisher) {
        this.workflowRepository = workflowRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Workflow> createAndStart(String workflowType, String correlationId,
                                         List<WorkflowStep> steps, Map<String, String> context,
                                         BusinessContext businessContext) {
        Workflow workflow = Workflow.create(
                businessContext.tenantId(), workflowType,
                correlationId, businessContext.actorId(), steps, context
        );
        workflow.start();
        return workflowRepository.save(workflow)
                .flatMap(this::publishEventsAndReturn);
    }

    @Override
    public Mono<Workflow> completeStep(WorkflowId workflowId,
                                       Map<String, String> outputData,
                                       BusinessContext context) {
        return workflowRepository.findById(workflowId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Workflow introuvable : " + workflowId)))
                .flatMap(workflow -> {
                    workflow.completeCurrentStep(outputData);
                    return workflowRepository.save(workflow)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Workflow> failStep(WorkflowId workflowId, String errorMessage,
                                   BusinessContext context) {
        return workflowRepository.findById(workflowId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Workflow introuvable : " + workflowId)))
                .flatMap(workflow -> {
                    workflow.failCurrentStep(errorMessage);
                    return workflowRepository.save(workflow)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Workflow> compensate(WorkflowId workflowId, BusinessContext context) {
        return workflowRepository.findById(workflowId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Workflow introuvable : " + workflowId)))
                .flatMap(workflow -> {
                    workflow.compensate();
                    return workflowRepository.save(workflow)
                            .flatMap(this::publishEventsAndReturn);
                });
    }

    @Override
    public Mono<Workflow> findById(WorkflowId workflowId) {
        return workflowRepository.findById(workflowId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Workflow introuvable : " + workflowId)));
    }

    @Override
    public Flux<Workflow> findRunningByTenant(TenantId tenantId) {
        return workflowRepository.findByTenantIdAndStatus(tenantId, WorkflowStatus.RUNNING);
    }

    private Mono<Workflow> publishEventsAndReturn(Workflow workflow) {
        return Mono.when(
                workflow.getDomainEvents().stream()
                        .map(eventPublisher::publish)
                        .toList()
        ).doOnSuccess(v -> workflow.clearDomainEvents())
         .thenReturn(workflow);
    }
}
