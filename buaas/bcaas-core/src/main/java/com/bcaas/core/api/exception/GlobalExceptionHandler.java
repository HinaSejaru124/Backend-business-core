package com.bcaas.core.api.exception;

import com.bcaas.core.api.dto.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Gestionnaire d'exceptions global pour l'API BCaaS.
 * Traduit les exceptions domaine en réponses HTTP structurées.
 * Analogie réseau : couche ICMP — messages d'erreur standardisés.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
                traceId(exchange),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage()
        );
        return Mono.just(ResponseEntity.badRequest().body(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalState(
            IllegalStateException ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
                traceId(exchange),
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(
            WebExchangeBindException ex, ServerWebExchange exchange) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getAllErrors().stream()
                .filter(e -> e instanceof FieldError)
                .map(e -> (FieldError) e)
                .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        ErrorResponse error = ErrorResponse.withFieldErrors(
                traceId(exchange),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "VALIDATION_ERROR",
                "Données de la requête invalides",
                fieldErrors
        );
        return Mono.just(ResponseEntity.unprocessableEntity().body(error));
    }

    @ExceptionHandler(SecurityException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleSecurity(
            SecurityException ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
                traceId(exchange),
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Accès refusé"
        );
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
            Exception ex, ServerWebExchange exchange) {
        ErrorResponse error = ErrorResponse.of(
                traceId(exchange),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Une erreur interne s'est produite"
        );
        return Mono.just(ResponseEntity.internalServerError().body(error));
    }

    private String traceId(ServerWebExchange exchange) {
        Object ctx = exchange.getAttributes().get("BUSINESS_CONTEXT");
        if (ctx instanceof com.bcaas.core.context.domain.BusinessContext bc) {
            return bc.traceId();
        }
        return UUID.randomUUID().toString();
    }
}
