package com.yowyob.businesscore.domain.port.internal;

import com.yowyob.businesscore.domain.shared.TypeEtape;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Port interne (stratégie) — fournit la séquence ordonnée d'étapes-types d'une opération déclarée.
 */
public interface PlanificateurDOperation {

    Flux<TypeEtape> planifier(UUID operationId);
}
