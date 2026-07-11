package com.yowyob.businesscore.adapter.in.rest.actor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Inscription libre-service d'un acteur métier (crée son identité kernel + le rattache)")
public record InscrireActeurRequete(
        @Schema(description = "Rôle métier à assigner (catégorie OPERATEUR)") @NotNull UUID roleMetierId,
        @Schema(example = "jean@pharma.cm") @NotBlank String email,
        @Schema(description = "Mot de passe (relayé au kernel, jamais stocké)") @NotBlank String password,
        @Schema(example = "Jean") @NotBlank String firstName,
        @Schema(example = "Kamdem") @NotBlank String lastName
) {
}
