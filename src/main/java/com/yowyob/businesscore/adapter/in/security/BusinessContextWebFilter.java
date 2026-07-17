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

import java.util.Optional;

/**
 * Après authentification, copie le {@link BusinessContext} et, en flux JWT, le token brut kernel
 * dans le Reactor Context ({@link BusinessContextHolder}, {@link KernelTokenHolder}) pour les
 * use cases, le pool R2DBC tenant-aware et {@code KernelClient}.
 */
public class BusinessContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // chain.filter(exchange) doit être souscrit EXACTEMENT une fois : c'est lui qui exécute tout le
        // reste du pipeline (filtres suivants + handler + écriture de la réponse). L'ancienne forme
        // `flatMap(auth -> chain.filter(...).contextWrite(...)).switchIfEmpty(chain.filter(...))` en
        // souscrivait DEUX fois pour toute requête authentifiée : le chemin nominal renvoie un Mono<Void>,
        // toujours « vide » au sens de switchIfEmpty (aucun onNext), donc switchIfEmpty relançait
        // chain.filter — d'où une double exécution du handler, une double écriture de la réponse
        // (UnsupportedOperationException : setContentLength sur en-têtes read-only) et, selon le filtre,
        // une fuite de connexion R2DBC finissant par geler le serveur. Le Mono.defer ne corrigeait rien :
        // il ne fait que retarder l'assemblage, pas empêcher le second abonnement.
        //
        // On résout donc d'abord l'authentification éventuelle en Optional (toujours exactement un
        // élément grâce à defaultIfEmpty), puis un unique flatMap appelle chain.filter une seule fois,
        // avec ou sans enrichissement du Reactor Context selon la présence du BusinessContext.
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated()
                        && auth.getPrincipal() instanceof BusinessContext)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(authOpt -> {
                    if (authOpt.isEmpty()) {
                        return chain.filter(exchange);
                    }
                    var auth = authOpt.get();
                    BusinessContext businessContext = (BusinessContext) auth.getPrincipal();
                    String jwtDelegue = resoudreBearer(auth, exchange);
                    return chain.filter(exchange)
                            .contextWrite(ctx -> {
                                Context enrichi = BusinessContextHolder.withContext(ctx, businessContext);
                                return jwtDelegue != null
                                        ? KernelTokenHolder.withToken(enrichi, jwtDelegue)
                                        : enrichi;
                            });
                });
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
