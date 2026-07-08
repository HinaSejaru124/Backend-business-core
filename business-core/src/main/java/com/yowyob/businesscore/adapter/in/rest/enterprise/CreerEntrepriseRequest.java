package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

@Schema(description = "Corps de création d'une entreprise (instance d'un type métier)")
public record CreerEntrepriseRequest(
        @Schema(description = "Identifiant du type métier", example = "00000000-0000-0000-0000-000000000000")
        @NotNull UUID typeId,
        @Schema(description = "Numéro de version épinglée", example = "1")
        @NotNull @Positive Integer versionNumber,
        @Schema(description = "Nom affiché de l'entreprise", example = "Boutique Alpha")
        @NotBlank String nom,
        @Schema(description = "Organisation kernel existante (optionnel)", example = "00000000-0000-0000-0000-000000000000")
        UUID organizationId
) {
}
