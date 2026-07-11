package com.yowyob.businesscore.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRouteClassifierTest {

    @Test
    @DisplayName("routes publiques")
    void publiques() {
        assertThat(AuthRouteClassifier.classify("/health")).isEqualTo(AuthRouteClassifier.AuthSurface.PUBLIC);
        assertThat(AuthRouteClassifier.classify("/v1/registration")).isEqualTo(AuthRouteClassifier.AuthSurface.PUBLIC);
        assertThat(AuthRouteClassifier.classify("/v1/auth/login")).isEqualTo(AuthRouteClassifier.AuthSurface.PUBLIC);
    }

    @Test
    @DisplayName("console développeur : gestion de la plateforme (front Business Core, JWT uniquement)")
    void console() {
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/api-keys"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/dashboard")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/auth/me")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/businesses")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/business-types")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        // Déclaration d'une opération/règle au niveau TYPE (template) : administration, pas runtime.
        assertThat(AuthRouteClassifier.classify("/v1/business-types/{typeId}/versions/{versionNumber}/operations"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        // Acteurs et règles locales d'une entreprise : administration du business par son propriétaire.
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/actors"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/rules"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
    }

    @Test
    @DisplayName("API intégration : usage runtime d'une entreprise par son backend terminal")
    void integration() {
        assertThat(AuthRouteClassifier.classify("/v1/sync")).isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/me")).isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/operations"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/operations/{name}:execute"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/traces"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/transactions"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/orders/{orderId}"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
    }

    @Test
    @DisplayName("API à clé seule : inscription/connexion acteur, Bearer refusé à l'application")
    void cleSeule() {
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/actors:login"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_CLE_SEULE);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/actors:register"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.API_CLE_SEULE);
        // Ne doit pas déborder sur le CRUD acteurs (JWT-only) ni sur /actors/me (JWT-only, cf. controller).
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/actors"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/businesses/{businessId}/actors/me"))
                .isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
    }
}
