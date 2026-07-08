package com.yowyob.businesscore.adapter.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Identifiants de connexion kernel")
public record LoginRequest(
        @Schema(description = "E-mail (champ `principal` attendu par le kernel)", example = "dev@example.com")
        @NotBlank String principal,
        @Schema(description = "Mot de passe (relayé au kernel, jamais stocké)", example = "••••••••")
        @NotBlank String password
) {
}
