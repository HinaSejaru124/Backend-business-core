package com.pharmacore.pharmaciebackend.config;

import com.pharmacore.pharmaciebackend.bcaas.BcaasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Relaie les erreurs Business Core (RFC 7807) telles quelles au frontend Pharmacie — jamais
 * reformulées de façon à masquer violatedRule/requiredAction/requiredDocument (cf. frontend-test.md §7).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BcaasException.class)
    public ProblemDetail gererBcaas(BcaasException e) {
        log.warn("Erreur Business Core : HTTP {} — {} — {}", e.status(), e.title(), e.detail());
        HttpStatusCode status = HttpStatusCode.valueOf(e.status() > 0 ? e.status() : 502);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, e.detail());
        pd.setTitle(e.title());
        if (e.violatedRule() != null) pd.setProperty("violatedRule", e.violatedRule());
        if (e.requiredAction() != null) pd.setProperty("requiredAction", e.requiredAction());
        if (e.requiredDocument() != null) pd.setProperty("requiredDocument", e.requiredDocument());
        return pd;
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail gererIntrouvable(RessourceIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail gererValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "Requête invalide" : detail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail gererMethodeNonSupportee(HttpRequestMethodNotSupportedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
    }

    /** Filet de sécurité : toute exception non gérée est loguée en entier (jamais silencieuse). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail gererGenerique(Exception e) {
        log.error("Erreur non gérée", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue");
    }
}
