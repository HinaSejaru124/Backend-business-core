package com.yowyob.businesscore.adapter.in.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extrait un JWT kernel de l'en-tête {@code Authorization: Bearer <token>}. Aucun Bearer → Mono.empty
 * (la requête tombe sur le filtre suivant : clé Business Core, sinon rejet par l'autorisation).
 */
@Component
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String PREFIXE = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String entete = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (entete == null || !entete.startsWith(PREFIXE)) {
            return Mono.empty();
        }
        String token = entete.substring(PREFIXE.length()).trim();
        if (token.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(JwtAuthenticationToken.unauthenticated(token));
    }
}
