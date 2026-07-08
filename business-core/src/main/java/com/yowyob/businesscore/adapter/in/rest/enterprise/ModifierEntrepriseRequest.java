package com.yowyob.businesscore.adapter.in.rest.enterprise;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de {@code PUT /v1/businesses/{businessId}} — modification des métadonnées locales.
 */
public record ModifierEntrepriseRequest(@NotBlank String nom) {
}
