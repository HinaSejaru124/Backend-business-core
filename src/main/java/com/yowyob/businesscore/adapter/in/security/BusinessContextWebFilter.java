package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.security.KernelTokenHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Après authentification, copie le {@link BusinessContext} et, en flux JWT, le token brut kernel
 * dans le Reactor Context ({@link BusinessContextHolder}, {@link KernelTokenHolder}) pour les
 * use cases, le pool R2DBC tenant-aware et {@code KernelClient}.
 */
public class BusinessContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated()
                        && auth.getPrincipal() instanceof BusinessContext)
                .flatMap(auth -> {
                    BusinessContext businessContext = (BusinessContext) auth.getPrincipal();
                    String jwtDelegue = resoudreBearer(auth, exchange);
                    return chain.filter(exchange)
                            .contextWrite(ctx -> {
                                Context enrichi = BusinessContextHolder.withContext(ctx, businessContext);
                                return jwtDelegue != null
                                        ? KernelTokenHolder.withToken(enrichi, jwtDelegue)
                                        : enrichi;
                            });
                })
                // Mono.defer : l'argument de switchIfEmpty est évalué avec empressement par Reactor
                // (avant même de savoir si la branche sera prise). Sans defer, chain.filter(exchange)
                // — donc TOUTE la suite du pipeline — s'exécutait une fois de trop à chaque requête,
                // en plus de l'exécution réelle faite par flatMap. Cf. doc Reactor sur switchIfEmpty.
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    /**
     * JWT depuis les credentials (flux JWT) ou, en secours, l'en-tête {@code Authorization} de la requête
     * (cas Bearer + clé BC sans écrasement du token).
     */
    private static String resoudreBearer(org.springframework.security.core.Authentication auth,
                                         ServerWebExchange exchange) {
        if (auth.getCredentials() instanceof String token && !token.isBlank()) {
            return token;
        }
        return extraireBearer(exchange);
    }

    private static String extraireBearer(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }
}
