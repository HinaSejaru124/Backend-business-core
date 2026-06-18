package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.enterprise.Entreprise;

import java.util.UUID;

/**
 * Réponse d'une entreprise (aligné OpenAPI {@code Business}).
 */
public record EntrepriseResponse(
        UUID id,
        String nom,
        UUID typeId,
        int versionNumber,
        UUID organizationId,
        String cycleVie
) {

    public static EntrepriseResponse depuis(Entreprise entreprise) {
        return new EntrepriseResponse(
                entreprise.id(),
                entreprise.nom(),
                entreprise.typeMetierId(),
                entreprise.numeroVersion(),
                entreprise.organizationId(),
                entreprise.cycleVie().name());
    }
}
