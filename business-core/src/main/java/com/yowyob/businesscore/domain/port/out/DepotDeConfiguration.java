package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — lit/écrit les paramètres qui se reflètent côté kernel.
 * Mappe : GET/PUT /api/settings/organizations/{orgId}/operational-policy.
 */
public interface DepotDeConfiguration {

    Mono<String> lire(UUID organizationId, String cle);

    Mono<Void> ecrire(UUID organizationId, String cle, String valeur);
}
