package com.yowyob.businesscore.domain.port.internal;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interne (stratégie) — résout les identifiants kernel d'une entreprise en un {@link ContexteKernel}.
 *
 * <p>Centralise la provenance des identifiants non métier (organization, agence, caisse, caissier,
 * devise) pour que les adapters kernel ne les devinent pas. L'implémentation combine BusinessContext,
 * entité Entreprise, Configuration et résolution auto de l'agence.
 */
public interface ResolveurContexteKernel {

    Mono<ContexteKernel> resoudre(UUID businessId);
}
