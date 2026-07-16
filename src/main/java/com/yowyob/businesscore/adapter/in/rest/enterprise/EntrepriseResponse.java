package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Entreprise (instance de métier)")
public record EntrepriseResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "Boutique Alpha") String nom,
        @Schema(description = "Type métier source") UUID typeId,
        @Schema(example = "1") int versionNumber,
        @Schema(description = "Organisation kernel liée") UUID organizationId,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDUE", "FERMEE"}) String cycleVie
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
