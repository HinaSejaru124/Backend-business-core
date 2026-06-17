package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.businesstype.TypeMetier;

import java.util.UUID;

/** Réponse JSON pour un TypeMetier. */
public record TypeMetierResponse(
        UUID   id,
        UUID   tenantId,
        UUID   businessDomainId,
        String code,
        String nom,
        String statut
) {
    public static TypeMetierResponse depuis(TypeMetier t) {
        return new TypeMetierResponse(
                t.id(), t.tenantId(), t.businessDomainId(),
                t.code(), t.nom(), t.statut().name()
        );
    }
}
