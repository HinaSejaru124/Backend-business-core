package com.yowyob.businesscore.adapter.in.rest.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de {@code POST /v1/auth/login}. {@code principal} = l'e-mail (le kernel attend bien
 * {@code principal}, pas {@code email}). Le mot de passe est relayé au kernel, jamais stocké.
 */
public record LoginRequest(
        @NotBlank String principal,
        @NotBlank String password
) {
}
