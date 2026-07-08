package com.yowyob.businesscore.adapter.in.rest.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Rattachement d'une personne à un rôle métier")
public record RattacherActeurRequete(
        @Schema(description = "Rôle métier cible", example = "00000000-0000-0000-0000-000000000000")
        @NotNull UUID roleMetierId,
        @Schema(description = "Identifiant personne kernel", example = "user-42")
        @NotBlank String identifiantPersonne
) {
}
