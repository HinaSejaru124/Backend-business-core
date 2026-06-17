package com.yowyob.businesscore.adapter.in.rest.operation;

import java.util.Map;
import java.util.UUID;

/**
 * Réponse {@code 200} d'une opération immédiate terminée (aligné OpenAPI {@code OperationResult}).
 */
public record OperationResultResponse(
        String statut,            // COMPLETEE
        String transactionId,     // référence de transaction kernel produite
        UUID traceId,
        Map<String, Object> details
) {
}
