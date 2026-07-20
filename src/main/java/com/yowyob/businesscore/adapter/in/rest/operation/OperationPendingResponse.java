package com.yowyob.businesscore.adapter.in.rest.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Réponse 202 — opération différée acceptée")
public record OperationPendingResponse(
        @Schema(example = "EN_COURS") String statut,
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID traceId,
        @Schema(description = "URL de suivi", example = "/v1/applications/{businessId}/traces/{traceId}")
        String suivi
) {
}
