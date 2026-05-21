package com.bcaas.core.api.dto.error;

import java.time.Instant;
import java.util.List;

/**
 * Format d'erreur standardisé pour toutes les réponses d'erreur de l'API.
 * Analogie réseau : format de message d'erreur ICMP — structure uniforme.
 */
public record ErrorResponse(
        String traceId,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String traceId, int status,
                                   String error, String message) {
        return new ErrorResponse(traceId, status, error, message, List.of(), Instant.now());
    }

    public static ErrorResponse withFieldErrors(String traceId, int status,
                                                String error, String message,
                                                List<FieldError> fieldErrors) {
        return new ErrorResponse(traceId, status, error, message, fieldErrors, Instant.now());
    }
}
