package com.yowyob.businesscore.adapter.in.rest.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valide (hors-ligne, sans contexte) que le motif d'URL d'exécution à suffixe d'action
 * {@code .../operations/{name}:execute} est bien supporté par le {@code PathPatternParser} de WebFlux
 * et que la variable {@code name} est extraite correctement (s'arrête au {@code :}).
 */
class OperationRoutePatternTest {

    @Test
    @DisplayName("le motif {name}:execute matche et extrait le nom d'opération")
    void motif_execute_matche() {
        PathPatternParser parser = new PathPatternParser();
        PathPattern pattern = parser.parse("/v1/applications/{businessId}/operations/{name}:execute");

        PathContainer chemin = PathContainer.parsePath(
                "/v1/applications/3f/operations/vente:execute");

        assertThat(pattern.matches(chemin)).isTrue();
        PathPattern.PathMatchInfo info = pattern.matchAndExtract(chemin);
        assertThat(info).isNotNull();
        assertThat(info.getUriVariables().get("businessId")).isEqualTo("3f");
        assertThat(info.getUriVariables().get("name")).isEqualTo("vente");
    }
}
