package com.yowyob.businesscore.adapter.in.rest.trace;

import com.yowyob.businesscore.domain.transaction.TraceOperation;

import java.time.Instant;
import java.util.UUID;

/**
 * Réponse d'une trace d'opération (aligné OpenAPI {@code OperationTrace}).
 */
public record OperationTraceResponse(
        UUID id,
        String operationNom,
        String cleIdempotence,
        UUID transactionKernelId,
        String statut,
        Instant creeLe,
        Instant resoluLe
) {

    public static OperationTraceResponse depuis(TraceOperation trace) {
        return new OperationTraceResponse(
                trace.id(),
                trace.operationNom(),
                trace.cleIdempotence(),
                trace.transactionKernelId(),
                trace.statut().name(),
                trace.creeLe(),
                trace.resoluLe());
    }
}
