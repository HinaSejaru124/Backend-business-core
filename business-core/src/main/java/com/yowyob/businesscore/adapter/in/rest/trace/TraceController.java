package com.yowyob.businesscore.adapter.in.rest.trace;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.transaction.ConsulterTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Consultation", description = "Transactions et traces")
@RestController
@RequestMapping("/v1")
public class TraceController {

    private final ConsulterTraceService consulterTrace;

    public TraceController(ConsulterTraceService consulterTrace) {
        this.consulterTrace = consulterTrace;
    }

    @Operation(summary = "Lister les traces d'opération",
            description = "Historique des exécutions (synchrones et différées) pour l'entreprise.")
    @ApiResponse(responseCode = "200", description = "Liste des traces")
    @GetMapping("/businesses/{businessId}/traces")
    public Flux<OperationTraceResponse> lister(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> consulterTrace.listerParEntreprise(businessId, ctx))
                .map(OperationTraceResponse::depuis);
    }

    @Operation(summary = "Suivre une opération différée",
            description = "Consulte l'état d'une trace renvoyée en `202` lors d'une exécution différée.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "État de la trace"),
            @ApiResponse(responseCode = "404", description = "Trace introuvable")
    })
    @GetMapping("/businesses/{businessId}/traces/{traceId}")
    public Mono<OperationTraceResponse> trouver(
            @PathVariable UUID businessId,
            @Parameter(description = "Identifiant de trace renvoyé par l'exécution")
            @PathVariable UUID traceId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> consulterTrace.trouver(businessId, traceId, ctx))
                .map(OperationTraceResponse::depuis);
    }
}
