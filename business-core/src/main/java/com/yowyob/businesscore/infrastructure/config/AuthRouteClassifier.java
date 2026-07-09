package com.yowyob.businesscore.infrastructure.config;

import java.util.Set;

/**
 * Classifie les routes REST selon la surface d'authentification attendue.
 *
 * <ul>
 *   <li>{@link AuthSurface#PUBLIC} — sans auth</li>
 *   <li>{@link AuthSurface#CONSOLE_JWT} — console développeur (humain) : Bearer uniquement</li>
 *   <li>{@link AuthSurface#API_INTEGRATION} — API produit : Bearer (kernel) + headers {@code X-BC-*} documentés</li>
 * </ul>
 */
public final class AuthRouteClassifier {

    public enum AuthSurface {
        PUBLIC,
        CONSOLE_JWT,
        API_INTEGRATION
    }

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/health",
            "/v1/registration",
            "/v1/auth/login",
            "/v1/auth/discover",
            "/v1/auth/select"
    );

    private static final Set<String> CONSOLE_PREFIXES = Set.of(
            "/v1/api-keys",
            "/v1/dashboard"
    );

    private AuthRouteClassifier() {
    }

    public static AuthSurface classify(String path) {
        if (path == null || path.isBlank()) {
            return AuthSurface.API_INTEGRATION;
        }
        String normalise = path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;
        if (PUBLIC_EXACT.contains(normalise)) {
            return AuthSurface.PUBLIC;
        }
        for (String prefix : CONSOLE_PREFIXES) {
            if (normalise.equals(prefix) || normalise.startsWith(prefix + "/")) {
                return AuthSurface.CONSOLE_JWT;
            }
        }
        if (normalise.startsWith("/v1/auth/")) {
            return AuthSurface.CONSOLE_JWT;
        }
        return AuthSurface.API_INTEGRATION;
    }
}
