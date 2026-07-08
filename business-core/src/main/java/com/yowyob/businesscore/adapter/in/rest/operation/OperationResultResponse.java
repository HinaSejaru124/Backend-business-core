package com.yowyob.businesscore.adapter.in.rest.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Réponse 200 — opération synchrone terminée")
public record OperationResultResponse(
        @Schema(example = "COMPLETEE") String statut,
        @Schema(description = "Identifiant transaction kernel", example = "11111111-1111-1111-0000-000000000000")
        String transactionId,
        @Schema(example = "11111111-1111-1111-0000-000000000000") UUID traceId,
        @Schema(description = "Détails métier retournés par la saga")
        Map<String, Object> details
) {
}
