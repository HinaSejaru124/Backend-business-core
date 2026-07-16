package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.in.rest.error.ProblemResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Réponse 403 en application/problem+json quand l'acteur est authentifié mais non autorisé. */
@Component
public class ProblemAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ProblemResponseWriter writer;

    public ProblemAccessDeniedHandler(ProblemResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
        return writer.write(exchange, HttpStatus.FORBIDDEN, "Accès refusé",
                "Vous n'avez pas les droits pour cette action");
    }
}
