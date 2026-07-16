package com.yowyob.businesscore.domain.offer;

import com.yowyob.businesscore.domain.shared.TypeCapacite;

import java.util.Objects;
import java.util.UUID;

/** Capacité activable d'une offre (un des 4 types). Combinables entre elles. */
public record Capacite(
        UUID id,
        UUID definitionOffreId,
        TypeCapacite type,
        boolean active
) {
    public Capacite {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(type, "type de capacité requis");
    }

    public static Capacite nouvelle(UUID id, UUID definitionOffreId, TypeCapacite type, boolean active) {
        return new Capacite(id, definitionOffreId, type, active);
    }
}
