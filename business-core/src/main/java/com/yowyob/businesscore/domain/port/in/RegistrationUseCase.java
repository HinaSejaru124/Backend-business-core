package com.yowyob.businesscore.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Port d'entrée (famille Accès) — inscription d'un développeur.
 * Provisionne en coulisse une ClientApplication kernel et émet la clé Business Core.
 * Implémenté par le socle (infra partagée).
 */
public interface RegistrationUseCase {

    Mono<ApiKeyEmise> inscrire(String nom, String email, String planCode);
}
