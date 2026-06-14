package com.yowyob.businesscore.adapter.in.rest.access;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corps de POST /v1/registration. Validé (@Valid) : échec -> 400 RFC 7807. */
public record RegistrationRequest(
        @NotBlank(message = "le nom est obligatoire") String nom,
        @Email(message = "email invalide") @NotBlank(message = "l'email est obligatoire") String email,
        String planCode
) {
}
