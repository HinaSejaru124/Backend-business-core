package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Port de sortie — empêche le double traitement d'une opération (idempotence).
 * Implémenté par le socle (Redis). Utilisé surtout par la brique Opérations.
 */
public interface VerrouDIdempotence {

    /** Tente d'acquérir le verrou ; renvoie true si acquis (première exécution), false sinon. */
    Mono<Boolean> acquerir(String cleIdempotence, Duration ttl);

    Mono<Void> liberer(String cleIdempotence);
}
