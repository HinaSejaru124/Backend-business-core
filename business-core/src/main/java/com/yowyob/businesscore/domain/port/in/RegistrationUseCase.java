package com.yowyob.businesscore.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Port d'entrée (famille Accès) — inscription d'un développeur.
 * 1. Crée le compte sur le kernel (sign-up)
 * 2. Émet la clé Business Core (bcClientId + bcApiKey)
 * Le dev vérifie son email, puis se connecte via /v1/auth/discover + /v1/auth/select.
 */
public interface RegistrationUseCase {

    Mono<ApiKeyEmise> inscrire(String firstName, String lastName,
                               String email, String password,
                               String planCode);
}
