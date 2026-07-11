package com.yowyob.businesscore.infrastructure.config;

import java.util.Set;

/**
 * Classifie les routes REST selon la surface d'authentification attendue — reflète exactement le
 * périmètre du filtre de clé API dans {@code SecurityConfig} (ROUTES_INTEGRATION_TERMINAL).
 *
 * <ul>
 *   <li>{@link AuthSurface#PUBLIC} — sans auth</li>
 *   <li>{@link AuthSurface#API_INTEGRATION} — usage runtime d'une entreprise par son backend terminal
 *       (synchronisation, opérations, traces, transactions) : Bearer (kernel) + headers {@code X-BC-*}
 *       documentés</li>
 *   <li>{@link AuthSurface#CONSOLE_JWT} — gestion de la plateforme (types métier, entreprises, clés,
 *       dashboard...), consommée par le front Business Core : Bearer uniquement</li>
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

    private static final Set<String> API_INTEGRATION_PREFIXES = Set.of(
            "/v1/sync",
            "/v1/businesses/me",
            "/v1/businesses/{businessId}/operations",
            "/v1/businesses/{businessId}/traces",
            "/v1/businesses/{businessId}/transactions",
            "/v1/businesses/{businessId}/orders"
    );

    private AuthRouteClassifier() {
    }

    public static AuthSurface classify(String path) {
        if (path == null || path.isBlank()) {
            return AuthSurface.CONSOLE_JWT;
        }
        String normalise = path.endsWith("/") && path.length() > 1
                ? path.substring(0, path.length() - 1)
                : path;
        if (PUBLIC_EXACT.contains(normalise)) {
            return AuthSurface.PUBLIC;
        }
        for (String prefix : API_INTEGRATION_PREFIXES) {
            if (normalise.equals(prefix) || normalise.startsWith(prefix + "/")) {
                return AuthSurface.API_INTEGRATION;
            }
        }
        return AuthSurface.CONSOLE_JWT;
    }
}
