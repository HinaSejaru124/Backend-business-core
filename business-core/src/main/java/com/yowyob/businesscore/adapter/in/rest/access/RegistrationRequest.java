package com.yowyob.businesscore.adapter.in.rest.access;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(
        @NotBlank(message = "le prénom est obligatoire") String firstName,
        @NotBlank(message = "le nom est obligatoire") String lastName,
        @Email(message = "email invalide") @NotBlank(message = "l'email est obligatoire") String email,
        @NotBlank(message = "le mot de passe est obligatoire") String password,
        String planCode
) {}