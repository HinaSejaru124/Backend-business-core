package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Surcharge locale d'un paramètre de configuration au niveau Application")
public record SurchargerParametreRequest(
        @Schema(description = "Nouvelle valeur", example = "18.5")
        @NotBlank(message = "valeur est obligatoire") String valeur
) {}
