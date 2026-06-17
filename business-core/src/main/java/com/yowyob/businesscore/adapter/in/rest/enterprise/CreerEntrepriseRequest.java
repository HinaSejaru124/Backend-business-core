package com.yowyob.businesscore.adapter.in.rest.enterprise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Corps de {@code POST /v1/businesses} (aligné OpenAPI {@code CreateBusiness}). Instancie un Type Métier
 * à une version donnée. {@code organizationId} (référence kernel) est optionnel au stade minimal.
 */
public record CreerEntrepriseRequest(
        @NotNull UUID typeId,
        @NotNull @Positive Integer versionNumber,
        @NotBlank String nom,
        UUID organizationId
) {
}
