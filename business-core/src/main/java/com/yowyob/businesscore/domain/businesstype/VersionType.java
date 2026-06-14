package com.yowyob.businesscore.domain.businesstype;

import java.time.Instant;
import java.util.UUID;

/**
 * Version immuable d'un Type Métier (coquille socle). Une entreprise reste épinglée à sa version.
 * Dev 2 ajoute la logique de publication (incrément du numéro, immuabilité — RG-03).
 */
public record VersionType(
        UUID id,
        UUID tenantId,
        UUID typeMetierId,
        int numero,
        boolean immuable,
        Instant publieeLe
) {
}
