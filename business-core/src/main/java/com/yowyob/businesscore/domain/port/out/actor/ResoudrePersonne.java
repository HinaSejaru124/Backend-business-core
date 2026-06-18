package com.yowyob.businesscore.domain.port.out.actor;

import reactor.core.publisher.Mono;

import java.util.UUID;

/** Opérateur (interne). Impl : POST /api/actors. Renvoie l'actorId kernel. */
public interface ResoudrePersonne {
    Mono<UUID> resoudreOperateur(String identifiant);
}
