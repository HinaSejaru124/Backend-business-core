package com.yowyob.businesscore.adapter.in.rest.operation;

import java.util.UUID;

/**
 * Réponse {@code 202} d'une opération différée acceptée (aligné OpenAPI {@code OperationPending}).
 */
public record OperationPendingResponse(
        String statut,   // EN_COURS
        UUID traceId,
        String suivi     // URL de suivi de la trace
) {
}
