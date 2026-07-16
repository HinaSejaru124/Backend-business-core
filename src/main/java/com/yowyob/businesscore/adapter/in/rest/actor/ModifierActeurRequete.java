package com.yowyob.businesscore.adapter.in.rest.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Changement de rôle d'un acteur")
public record ModifierActeurRequete(
        @Schema(description = "Nouveau rôle métier", example = "00000000-0000-0000-0000-000000000000")
        @NotNull UUID roleMetierId
) {
}
