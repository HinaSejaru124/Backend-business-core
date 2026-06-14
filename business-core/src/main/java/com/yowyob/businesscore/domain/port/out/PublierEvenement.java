package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Port de sortie — publie un événement (opérations différées, audit).
 * Implémenté par le socle (bus Kafka).
 */
public interface PublierEvenement {

    Mono<Void> publier(String type, Object charge);
}
