package com.yowyob.businesscore.infrastructure.config;

import java.util.Set;

/**
 * Classifie les routes REST selon la surface d'authentification attendue — reflète exactement le
 * périmètre du filtre de clé API dans {@code SecurityConfig} (ROUTES_INTEGRATION_TERMINAL) et les gardes
 * applicatives ({@code exigerCleEntreprise} dans {@code ActeurAuthController}).
 *
 * <ul>
 *   <li>{@link AuthSurface#PUBLIC} — sans auth</li>
 *   <li>{@link AuthSurface#API_INTEGRATION} — usage runtime d'une application par son backend terminal
 *       (synchronisation, opérations, traces, transactions) : Bearer (kernel) <b>ou</b> headers
 *       {@code X-BC-*}, documentés comme deux voies possibles</li>
 *   <li>{@link AuthSurface#API_CLE_SEULE} — même surface terminal, mais où le Bearer est délibérément
 *       <b>refusé</b> à l'application (un seul mode d'appel, sans ambiguïté) : pas de cadenas Swagger,
 *       seuls les headers {@code X-BC-*} sont documentés, marqués requis</li>
 *   <li>{@link AuthSurface#CONSOLE_JWT} — gestion de la plateforme (types métier, applications, clés,
 *       dashboard...), consommée par le front Business Core : Bearer uniquement</li>
 * </ul>
 */
public final class AuthRouteClassifier {

    public enum AuthSurface {
        PUBLIC,
        CONSOLE_JWT,
        API_INTEGRATION,
        API_CLE_SEULE
    }

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/health",
            "/v1/registration",
            "/v1/auth/login",
            "/v1/auth/discover",
            "/v1/auth/select"
    );

    /** Bearer refusé à l'application (cf. {@code ActeurAuthController.exigerCleEntreprise}) — exact only. */
    private static final Set<String> API_CLE_SEULE_EXACT = Set.of(
            "/v1/applications/{businessId}/actors:login",
            "/v1/applications/{businessId}/actors:register"
    );

    private static final Set<String> API_INTEGRATION_PREFIXES = Set.of(
            "/v1/sync",
            "/v1/applications/me",
            "/v1/applications/{businessId}/operations",
            "/v1/applications/{businessId}/traces",
            "/v1/applications/{businessId}/transactions",
            "/v1/applications/{businessId}/orders"
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
        if (API_CLE_SEULE_EXACT.contains(normalise)) {
            return AuthSurface.API_CLE_SEULE;
        }
        for (String prefix : API_INTEGRATION_PREFIXES) {
            if (normalise.equals(prefix) || normalise.startsWith(prefix + "/")) {
                return AuthSurface.API_INTEGRATION;
            }
        }
        return AuthSurface.CONSOLE_JWT;
    }
}
