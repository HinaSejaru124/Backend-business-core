package com.yowyob.businesscore.adapter.in.rest.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Modification du code d'un rôle métier")
public record ModifierRoleRequete(
        @Schema(description = "Nouveau code", example = "VENDEUR")
        @NotBlank String code
) {
}