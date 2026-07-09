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
    @DisplayName("console développeur")
    void console() {
        assertThat(AuthRouteClassifier.classify("/v1/api-keys")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/dashboard")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
        assertThat(AuthRouteClassifier.classify("/v1/auth/me")).isEqualTo(AuthRouteClassifier.AuthSurface.CONSOLE_JWT);
    }

    @Test
    @DisplayName("API intégration")
    void integration() {
        assertThat(AuthRouteClassifier.classify("/v1/businesses")).isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
        assertThat(AuthRouteClassifier.classify("/v1/business-types")).isEqualTo(AuthRouteClassifier.AuthSurface.API_INTEGRATION);
    }
}
