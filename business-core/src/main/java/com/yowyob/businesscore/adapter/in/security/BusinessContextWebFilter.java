package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Après authentification, copie le {@link BusinessContext} (porté par l'Authentication) dans le
 * Reactor Context, afin qu'il soit lisible par les use cases ET par le pool R2DBC tenant-aware
 * (Barrière 2/3). Ajouté juste après le filtre d'authentification dans la chaîne de sécurité.
 */
public class BusinessContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated()
                        && auth.getPrincipal() instanceof BusinessContext)
                .map(auth -> (BusinessContext) auth.getPrincipal())
                .flatMap(businessContext -> chain.filter(exchange)
                        .contextWrite(ctx -> BusinessContextHolder.withContext(ctx, businessContext)))
                .switchIfEmpty(chain.filter(exchange));
    }
}
