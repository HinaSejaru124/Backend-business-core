package com.yowyob.businesscore.adapter.in.rest.error;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.KernelException;
import com.yowyob.businesscore.application.error.ProblemException;

import reactor.core.publisher.Mono;

/**
 * Traduit les exceptions des controllers en réponses
 * {@code application/problem+json} (RFC 7807).
 * Les features lèvent {@link ProblemException} ; le format et les extensions
 * métier sont garantis ici.
 */
@RestControllerAdvice
public class GlobalProblemHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalProblemHandler.class);

        @ExceptionHandler(ProblemException.class)
        public Mono<ResponseEntity<ProblemDetail>> handleProblem(ProblemException ex, ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré : {}", ex.getDetail());
                        return Mono.empty();
                }
                return Mono.just(construireReponseProblem(ex));
        }

        /** Erreur HTTP brute du kernel (4xx/5xx) — expose le chemin exact de l'endpoint. */
        @ExceptionHandler(WebClientResponseException.class)
        public Mono<ResponseEntity<ProblemDetail>> handleKernelTransport(WebClientResponseException ex,
                        ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré kernel transport : {}", KernelClient.cheminRequete(ex));
                        return Mono.empty();
                }
                log.warn("Échec appel kernel : {} — {}", KernelClient.cheminRequete(ex),
                                KernelClient.detailReponse(ex));
                ProblemException problem = KernelClient.versErreurKernel(ex);
                ProblemDetail body = ProblemDetail.forStatusAndDetail(problem.getStatus(), problem.getDetail());
                body.setTitle(problem.getTitle());
                body.setProperty("kernelPath", cheminSansMethode(ex));
                problem.getExtensions().forEach(body::setProperty);
                return Mono.just(ResponseEntity.status(problem.getStatus())
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(body));
        }

        /** Erreur métier renvoyée par le kernel dans l'enveloppe ({@code errorCode} non nul). */
        @ExceptionHandler(KernelException.class)
        public Mono<ResponseEntity<ProblemDetail>> handleKernelMetier(KernelException ex, ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré kernel : {}", ex.getMessage());
                        return Mono.empty();
                }
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
                problem.setTitle("Erreur kernel");
                problem.setProperty("kernelErrorCode", ex.errorCode());
                return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(problem));
        }

        /** Échec de validation Bean Validation sur un DTO (@Valid) -> 400. */
        @ExceptionHandler(WebExchangeBindException.class)
        public Mono<ResponseEntity<ProblemDetail>> handleValidation(WebExchangeBindException ex,
                        ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré validation");
                        return Mono.empty();
                }
                String detail = ex.getFieldErrors().stream()
                                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                                .collect(Collectors.joining("; "));
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                                detail.isBlank() ? "Requête invalide" : detail);
                problem.setTitle("Requête invalide");
                return Mono.just(ResponseEntity.badRequest()
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(problem));
        }

        /** Corps de requête malformé / paramètre manquant -> 400. */
        @ExceptionHandler(ServerWebInputException.class)
        public Mono<ResponseEntity<ProblemDetail>> handleInput(ServerWebInputException ex,
                        ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré input");
                        return Mono.empty();
                }
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getReason());
                problem.setTitle("Requête invalide");
                return Mono.just(ResponseEntity.badRequest()
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(problem));
        }

        /**
         * Filet de sécurité : délègue aux handlers connus si l'exception est encapsulée
         * (ex. {@code RetryExhaustedException}), sinon 500 générique.
         */
        @ExceptionHandler(Exception.class)
        public Mono<ResponseEntity<ProblemDetail>> handleUnexpected(Exception ex, ServerWebExchange exchange) {
                if (exchange.getResponse().isCommitted()) {
                        log.warn("Réponse déjà commitée, ignoré exception : {}", ex.toString());
                        return Mono.empty();
                }
                ProblemException problemEx = chercherCause(ex, ProblemException.class);
                if (problemEx != null) {
                        return handleProblem(problemEx, exchange);
                }
                KernelException kernelEx = chercherCause(ex, KernelException.class);
                if (kernelEx != null) {
                        return handleKernelMetier(kernelEx, exchange);
                }
                WebClientResponseException transport = chercherCause(ex, WebClientResponseException.class);
                if (transport != null) {
                        return handleKernelTransport(transport, exchange);
                }
                log.error("Erreur interne non gérée", ex);
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Une erreur interne est survenue");
                problem.setTitle("Erreur interne");
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(problem));
        }

        private static ResponseEntity<ProblemDetail> construireReponseProblem(ProblemException ex) {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getDetail());
                problem.setTitle(ex.getTitle());
                ex.getExtensions().forEach(problem::setProperty);
                return ResponseEntity.status(ex.getStatus())
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body(problem);
        }

        private static String cheminSansMethode(WebClientResponseException ex) {
                if (ex.getRequest() == null || ex.getRequest().getURI() == null) {
                        return "?";
                }
                return ex.getRequest().getURI().getPath();
        }

        private static <T extends Throwable> T chercherCause(Throwable ex, Class<T> type) {
                Throwable courant = ex;
                while (courant != null) {
                        if (type.isInstance(courant)) {
                                return type.cast(courant);
                        }
                        courant = courant.getCause();
                }
                return null;
        }
}
