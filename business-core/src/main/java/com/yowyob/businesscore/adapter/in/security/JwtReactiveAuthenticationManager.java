package com.yowyob.businesscore.adapter.in.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Authentifie une requête portant un JWT kernel : vérifie le token (signature RS256 + exp + iss via
 * {@link JwksJwtVerifier}) puis construit le {@link BusinessContext} à partir des claims
 * ({@code tid}=tenant, {@code actor}=acteur, {@code permissions}=rôles). La vérification nimbus est
 * bloquante : exécutée sur {@code boundedElastic} pour rester non bloquant côté Reactor.
 */
@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwksJwtVerifier verifier;

    public JwtReactiveAuthenticationManager(JwksJwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return Mono.fromCallable(() -> {
                    JWTClaimsSet claims = verifier.verifier(token);
                    BusinessContext contexte = versContexte(claims);
                    return (Authentication) JwtAuthenticationToken.authenticated(token, contexte);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(erreur -> erreur instanceof BadCredentialsException
                        ? erreur
                        : new BadCredentialsException("JWT kernel invalide", erreur));
    }

    private BusinessContext versContexte(JWTClaimsSet claims) throws Exception {
        String tid = claims.getStringClaim("tid");
        if (tid == null || tid.isBlank()) {
            throw new BadCredentialsException("JWT sans claim tenant (tid)");
        }
        UUID tenantId;
        try {
            tenantId = UUID.fromString(tid);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Claim tid invalide");
        }
        UUID actorId = parseUuid(claims.getStringClaim("actor"));
        Set<String> roles = Set.copyOf(permissions(claims));
        String traceId = claims.getJWTID() != null ? claims.getJWTID() : UUID.randomUUID().toString();
        return new BusinessContext(tenantId, actorId, roles, null, traceId, Locale.getDefault());
    }

    private List<String> permissions(JWTClaimsSet claims) throws Exception {
        List<String> permissions = claims.getStringListClaim("permissions");
        if (permissions == null) {
            permissions = claims.getStringListClaim("authorities");
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
