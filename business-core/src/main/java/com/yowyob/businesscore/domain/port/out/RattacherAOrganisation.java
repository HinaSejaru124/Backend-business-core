package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — rattache un acteur à une organisation.
 * Mappe : POST /api/organizations/{orgId}/actors.
 */
public interface RattacherAOrganisation {

    Mono<Void> rattacher(UUID organizationId, UUID actorId);
}
