package com.yowyob.businesscore.adapter.in.rest.trace;

import com.yowyob.businesscore.domain.transaction.TraceOperation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Trace d'exécution d'une opération")
public record OperationTraceResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "vente") String operationNom,
        @Schema(description = "Clé d'idempotence fournie à l'exécution") String cleIdempotence,
        UUID transactionKernelId,
        @Schema(example = "COMPLETEE", allowableValues = {"EN_COURS", "COMPLETEE", "COMPENSEE"}) String statut,
        @Schema(description = "Règle/cause violée si le statut est COMPENSEE (ex. violatedRule de l'erreur)")
        String codeErreur,
        @Schema(description = "Message diagnostique si le statut est COMPENSEE") String messageErreur,
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
                trace.codeErreur(),
                trace.messageErreur(),
                trace.creeLe(),
                trace.resoluLe());
    }
}
