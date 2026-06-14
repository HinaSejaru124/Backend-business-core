package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — traduit un rôle métier en permissions techniques kernel.
 * Mappe : POST /api/roles puis POST /api/roles/assignments (deux appels).
 */
public interface AppliquerRoleTechnique {

    Mono<Void> appliquer(UUID actorId, String roleCode);
}
