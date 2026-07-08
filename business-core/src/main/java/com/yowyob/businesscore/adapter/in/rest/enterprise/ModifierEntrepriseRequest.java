package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Modification des métadonnées locales d'une entreprise")
public record ModifierEntrepriseRequest(
        @Schema(description = "Nouveau nom", example = "Boutique Alpha Plus")
        @NotBlank String nom
) {
}
