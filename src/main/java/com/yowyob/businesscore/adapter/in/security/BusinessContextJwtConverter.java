package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Convertit un {@link Jwt} kernel (déjà vérifié par le {@code ReactiveJwtDecoder} standard : signature
 * RS256 + exp + iss) en {@link JwtAuthenticationToken} dont le principal est le {@link BusinessContext}
 * (claims {@code tid}=tenant, {@code actor}=acteur, {@code permissions}=rôles) et les credentials le
 * token brut (pour re-transmission par {@code KernelClient}). Branché via
 * {@code oauth2ResourceServer().jwt().jwtAuthenticationConverter(...)}.
 */
@Component
public class BusinessContextJwtConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return Mono.fromCallable(() -> construire(jwt));
    }

    private AbstractAuthenticationToken construire(Jwt jwt) {
        String tid = jwt.getClaimAsString("tid");
        if (tid == null || tid.isBlank()) {
            throw new BadCredentialsException("JWT sans claim tenant (tid)");
        }
        UUID tenantId;
        try {
            tenantId = UUID.fromString(tid);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Claim tid invalide");
        }
        UUID actorId = parseUuid(jwt.getClaimAsString("actor"));
        Set<String> roles = Set.copyOf(permissions(jwt));
        String traceId = jwt.getId() != null ? jwt.getId() : UUID.randomUUID().toString();
        BusinessContext contexte = new BusinessContext(
                tenantId, actorId, roles, null, traceId, Locale.getDefault());
        return JwtAuthenticationToken.authenticated(jwt.getTokenValue(), contexte);
    }

    private List<String> permissions(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        if (permissions == null) {
            permissions = jwt.getClaimAsStringList("authorities");
        }
        return permissions == null ? List.of() : permissions;
    }

    private UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
