package com.yowyob.businesscore.adapter.in.rest.operation;

import com.yowyob.businesscore.domain.shared.TypeEtape;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Étape d'une opération dans le corps de déclaration (aligné OpenAPI {@code CreateOperation.etapes}).
 * {@code typeEtape} appartient au catalogue fermé {@link TypeEtape}.
 */
public record EtapeRequest(
        @NotNull @PositiveOrZero Integer ordre,
        @NotNull TypeEtape typeEtape
) {
}
