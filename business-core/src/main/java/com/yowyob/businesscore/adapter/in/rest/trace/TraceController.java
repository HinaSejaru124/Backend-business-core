package com.yowyob.businesscore.adapter.in.rest.trace;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.transaction.ConsulterTraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API REST — suivi des traces d'opération (Consultation). Routes (cf. OpenAPI) :
 * <ul>
 *   <li>{@code GET /v1/businesses/{businessId}/traces} — lister ;</li>
 *   <li>{@code GET /v1/businesses/{businessId}/traces/{traceId}} — suivre une opération différée.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1")
public class TraceController {

    private final ConsulterTraceService consulterTrace;

    public TraceController(ConsulterTraceService consulterTrace) {
        this.consulterTrace = consulterTrace;
    }

    @GetMapping("/businesses/{businessId}/traces")
    public Flux<OperationTraceResponse> lister(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> consulterTrace.listerParEntreprise(businessId, ctx))
                .map(OperationTraceResponse::depuis);
    }

    @GetMapping("/businesses/{businessId}/traces/{traceId}")
    public Mono<OperationTraceResponse> trouver(@PathVariable UUID businessId,
                                                @PathVariable UUID traceId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> consulterTrace.trouver(businessId, traceId, ctx))
                .map(OperationTraceResponse::depuis);
    }
}
