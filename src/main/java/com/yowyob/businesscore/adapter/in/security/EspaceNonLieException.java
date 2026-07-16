package com.yowyob.businesscore.adapter.in.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Clé API valide mais dont le compte développeur n'a pas encore de tenant kernel lié. Le dev doit se
 * connecter une fois ({@code POST /v1/auth/login}) pour que le {@code kernel_tenant_id} soit renseigné.
 * Traduite en 401 explicite par {@link ProblemAuthenticationEntryPoint}.
 */
public class EspaceNonLieException extends AuthenticationException {

    public EspaceNonLieException(String message) {
        super(message);
    }
}
