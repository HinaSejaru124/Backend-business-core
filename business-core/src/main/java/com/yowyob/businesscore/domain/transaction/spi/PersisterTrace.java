package com.yowyob.businesscore.domain.transaction.spi;

import com.yowyob.businesscore.domain.transaction.TraceOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie (feature Opérations) — persistance des {@link TraceOperation}.
 * Le filtrage tenant est garanti par la RLS. {@code cleIdempotence} est unique : un rejeu ne crée pas
 * de doublon.
 */
public interface PersisterTrace {

    Mono<TraceOperation> sauvegarder(TraceOperation trace);

    Mono<TraceOperation> trouverParId(UUID traceId);

    Mono<TraceOperation> trouverParCleIdempotence(String cleIdempotence);

    Flux<TraceOperation> listerParEntreprise(UUID entrepriseId);
}
