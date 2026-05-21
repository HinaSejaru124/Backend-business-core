package com.bcaas.core.api.rest.workflow;

import com.bcaas.core.api.dto.response.WorkflowResponse;
import com.bcaas.core.api.filter.BusinessContextFilter;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.workflow.port.input.WorkflowUseCase;
import com.bcaas.core.workflow.domain.model.WorkflowId;
import com.bcaas.core.workflow.domain.model.WorkflowStep;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows", description = "Moteur de workflow Saga — Couche 5 : Business Capabilities")
public class WorkflowController {

    private final WorkflowUseCase workflowUseCase;

    public WorkflowController(WorkflowUseCase workflowUseCase) {
        this.workflowUseCase = workflowUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer et démarrer un workflow")
    public Mono<ResponseEntity<WorkflowResponse>> createAndStart(
            @RequestParam String type,
            @RequestParam String correlationId,
            @RequestBody List<WorkflowStep> steps,
            ServerWebExchange exchange) {

        BusinessContext context = getContext(exchange);
        return workflowUseCase.createAndStart(type, correlationId, steps, Map.of(), context)
                .map(w -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(WorkflowResponse.from(w)));
    }

    @GetMapping("/{workflowId}")
    @Operation(summary = "Récupérer un workflow par ID")
    public Mono<ResponseEntity<WorkflowResponse>> findById(@PathVariable String workflowId) {
        return workflowUseCase.findById(WorkflowId.of(workflowId))
                .map(w -> ResponseEntity.ok(WorkflowResponse.from(w)));
    }

    @GetMapping("/running")
    @Operation(summary = "Lister les workflows en cours du tenant")
    public Flux<WorkflowResponse> findRunning(ServerWebExchange exchange) {
        BusinessContext context = getContext(exchange);
        return workflowUseCase.findRunningByTenant(context.tenantId())
                .map(WorkflowResponse::from);
    }

    @PutMapping("/{workflowId}/complete-step")
    @Operation(summary = "Compléter l'étape courante")
    public Mono<ResponseEntity<WorkflowResponse>> completeStep(
            @PathVariable String workflowId,
            @RequestBody(required = false) Map<String, String> outputData,
            ServerWebExchange exchange) {
        Map<String, String> output = outputData != null ? outputData : Map.of();
        return workflowUseCase.completeStep(WorkflowId.of(workflowId), output, getContext(exchange))
                .map(w -> ResponseEntity.ok(WorkflowResponse.from(w)));
    }

    @PutMapping("/{workflowId}/fail-step")
    @Operation(summary = "Marquer l'étape courante comme échouée")
    public Mono<ResponseEntity<WorkflowResponse>> failStep(
            @PathVariable String workflowId,
            @RequestParam String errorMessage,
            ServerWebExchange exchange) {
        return workflowUseCase.failStep(WorkflowId.of(workflowId), errorMessage, getContext(exchange))
                .map(w -> ResponseEntity.ok(WorkflowResponse.from(w)));
    }

    @PutMapping("/{workflowId}/compensate")
    @Operation(summary = "Lancer la compensation (Saga rollback)")
    public Mono<ResponseEntity<WorkflowResponse>> compensate(
            @PathVariable String workflowId,
            ServerWebExchange exchange) {
        return workflowUseCase.compensate(WorkflowId.of(workflowId), getContext(exchange))
                .map(w -> ResponseEntity.ok(WorkflowResponse.from(w)));
    }

    private BusinessContext getContext(ServerWebExchange exchange) {
        BusinessContext context = (BusinessContext) exchange.getAttributes()
                .get(BusinessContextFilter.CONTEXT_ATTRIBUTE);
        if (context == null) throw new SecurityException("BusinessContext manquant");
        return context;
    }
}
