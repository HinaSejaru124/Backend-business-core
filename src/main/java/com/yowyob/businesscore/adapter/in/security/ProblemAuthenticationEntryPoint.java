package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.in.rest.error.ProblemResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Réponse 401 en application/problem+json quand l'authentification manque ou échoue. */
@Component
public class ProblemAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ProblemResponseWriter writer;

    public ProblemAuthenticationEntryPoint(ProblemResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        if (ex instanceof EspaceNonLieException) {
            return writer.write(exchange, HttpStatus.UNAUTHORIZED, "Espace non lié", ex.getMessage());
        }
        return writer.write(exchange, HttpStatus.UNAUTHORIZED, "Authentification requise",
                "Clé Business Core absente ou invalide");
    }
}
