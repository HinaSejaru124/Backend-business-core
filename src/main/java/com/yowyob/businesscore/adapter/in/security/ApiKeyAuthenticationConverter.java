package com.yowyob.businesscore.adapter.in.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extrait la clé Business Core des en-têtes {@code X-BC-Client-Id} / {@code X-BC-Api-Key}
 * (et l'acteur asserté optionnel {@code X-BC-On-Behalf-Of}). Aucune clé -> Mono.empty (la requête
 * sera rejetée par l'autorisation si la route est protégée).
 */
@Component
public class ApiKeyAuthenticationConverter implements ServerAuthenticationConverter {

    public static final String HEADER_CLIENT_ID = "X-BC-Client-Id";
    public static final String HEADER_API_KEY = "X-BC-Api-Key";
    public static final String HEADER_ON_BEHALF_OF = "X-BC-On-Behalf-Of";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        // Bearer présent → le Resource Server JWT prend le relais ; ne pas écraser par la clé BC.
        if (aBearer(exchange)) {
            return Mono.empty();
        }
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String clientId = headers.getFirst(HEADER_CLIENT_ID);
        String apiKey = headers.getFirst(HEADER_API_KEY);
        if (clientId == null || clientId.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Mono.empty();
        }
        String onBehalfOf = headers.getFirst(HEADER_ON_BEHALF_OF);
        return Mono.just(ApiKeyAuthenticationToken.unauthenticated(clientId, apiKey, onBehalfOf));
    }

    private static boolean aBearer(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }
}
