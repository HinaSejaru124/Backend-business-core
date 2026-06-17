package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.businesstype.VersionType;

import java.time.Instant;
import java.util.UUID;

/** Réponse JSON pour une VersionType. */
public record VersionTypeResponse(
        UUID    id,
        UUID    typeMetierId,
        int     numero,
        boolean immuable,
        Instant publieeLe,
        String  libelle
) {
    public static VersionTypeResponse depuis(VersionType v) {
        return new VersionTypeResponse(
                v.id(), v.typeMetierId(), v.numero(),
                v.immuable(), v.publieeLe(), v.libelle()
        );
    }
}
