package com.yowyob.businesscore.adapter.in.rest.error;

import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.stream.Collectors;

/**
 * Traduit les exceptions des controllers en réponses {@code application/problem+json} (RFC 7807).
 * Les features lèvent {@link ProblemException} ; le format et les extensions métier sont garantis ici.
 */
@RestControllerAdvice
public class GlobalProblemHandler {

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ProblemDetail> handleProblem(ProblemException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getDetail());
        problem.setTitle(ex.getTitle());
        ex.getExtensions().forEach(problem::setProperty);
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Échec de validation Bean Validation sur un DTO (@Valid) -> 400. */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleValidation(WebExchangeBindException ex) {
        String detail = ex.getFieldErrors().stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "Requête invalide" : detail);
        problem.setTitle("Requête invalide");
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Corps de requête malformé / paramètre manquant -> 400. */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetail> handleInput(ServerWebInputException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getReason());
        problem.setTitle("Requête invalide");
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Filet de sécurité : toute exception inattendue -> 500 sans fuite de stacktrace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue");
        problem.setTitle("Erreur interne");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
