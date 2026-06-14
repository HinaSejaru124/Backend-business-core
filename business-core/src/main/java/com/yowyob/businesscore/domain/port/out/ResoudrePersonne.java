package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — résout/crée un opérateur interne (acteur).
 * Mappe : POST /api/actors.
 */
public interface ResoudrePersonne {

    Mono<UUID> resoudreOperateur(String identifiant, String nom);
}
