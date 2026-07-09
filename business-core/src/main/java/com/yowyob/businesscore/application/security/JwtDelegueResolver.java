package com.yowyob.businesscore.application.security;

import com.yowyob.businesscore.adapter.in.security.JwtAuthenticationToken;
import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Résout le JWT utilisateur à re-transmettre au kernel : Reactor Context d'abord
 * ({@link KernelTokenHolder}), puis SecurityContext (repli si la propagation du filtre a été perdue).
 */
public final class JwtDelegueResolver {

    private JwtDelegueResolver() {
    }

    public static Mono<Optional<String>> courant() {
        return KernelTokenHolder.currentToken()
                .flatMap(deja -> deja.isPresent()
                        ? Mono.just(deja)
                        : tokenDepuisSecurite());
    }

    private static Mono<Optional<String>> tokenDepuisSecurite() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(JwtDelegueResolver::extraireJwtDelegue)
                .defaultIfEmpty(Optional.empty());
    }

    private static Mono<Optional<String>> extraireJwtDelegue(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Mono.empty();
        }
        if (auth instanceof JwtAuthenticationToken && auth.getPrincipal() instanceof BusinessContext
                && auth.getCredentials() instanceof String token && !token.isBlank()) {
            return Mono.just(Optional.of(token));
        }
        return Mono.empty();
    }
}
