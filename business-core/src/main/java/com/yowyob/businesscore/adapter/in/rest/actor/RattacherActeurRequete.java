package com.yowyob.businesscore.adapter.in.rest.actor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Corps de {@code POST /v1/businesses/{businessId}/actors}. Rattache une personne (opérateur ou
 * bénéficiaire selon la catégorie du rôle) à l'entreprise.
 */
public record RattacherActeurRequete(
        @NotNull UUID roleMetierId,
        @NotBlank String identifiantPersonne) {
}
