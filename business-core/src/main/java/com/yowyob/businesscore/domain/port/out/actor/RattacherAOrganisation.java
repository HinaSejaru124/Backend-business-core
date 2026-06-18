package com.yowyob.businesscore.domain.port.out.actor;

import reactor.core.publisher.Mono;

import java.util.UUID;

/** Rattache un acteur à l'organisation. Impl : POST /api/organizations/{orgId}/actors. */
public interface RattacherAOrganisation {
    Mono<Void> rattacher(UUID organizationId, UUID acteurKernelId);
}
