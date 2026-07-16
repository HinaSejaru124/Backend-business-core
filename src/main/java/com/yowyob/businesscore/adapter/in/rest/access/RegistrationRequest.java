package com.yowyob.businesscore.adapter.in.rest.access;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Inscription d'un compte développeur")
public record RegistrationRequest(
        @Schema(example = "Isa") @NotBlank(message = "le prénom est obligatoire") String firstName,
        @Schema(example = "MAG") @NotBlank(message = "le nom est obligatoire") String lastName,
        @Schema(example = "Isa.MAG@example.com")
        @Email(message = "email invalide") @NotBlank(message = "l'email est obligatoire") String email,
        @Schema(example = "••••••••") @NotBlank(message = "le mot de passe est obligatoire") String password,
        @Schema(description = "Code plan (optionnel)", example = "FREE") String planCode
) {}
