package com.yowyob.businesscore.domain.port.out.actor;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Applique le rôle technique kernel à un opérateur.
 * Impl : POST /api/roles PUIS POST /api/roles/assignments (2 appels enchaînés).
 */
public interface AppliquerRoleTechnique {
    Mono<Void> appliquer(String codeRole, UUID actorKernelId);
}
