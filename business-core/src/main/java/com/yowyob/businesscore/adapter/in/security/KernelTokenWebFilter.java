package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.security.KernelTokenHolder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Après authentification par JWT, copie le token brut dans le Reactor Context ({@link KernelTokenHolder})
 * afin que {@code KernelClient} le re-transmette au kernel (Bearer délégué). N'agit que pour le flux JWT
 * (principal = {@link BusinessContext} ET credentials = token String) ; sans effet sur le flux clé BC.
 */
public class KernelTokenWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated()
                        && auth.getPrincipal() instanceof BusinessContext
                        && auth.getCredentials() instanceof String token && !token.isBlank())
                .map(auth -> (String) auth.getCredentials())
                .flatMap(token -> chain.filter(exchange)
                        .contextWrite(ctx -> KernelTokenHolder.withToken(ctx, token)))
                .switchIfEmpty(chain.filter(exchange));
    }
}
