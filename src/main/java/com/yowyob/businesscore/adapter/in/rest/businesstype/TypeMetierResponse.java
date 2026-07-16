package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Type métier déclaré")
public record TypeMetierResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        UUID tenantId,
        UUID businessDomainId,
        @Schema(example = "RETAIL") String code,
        @Schema(example = "Commerce de détail") String nom,
        @Schema(example = "BROUILLON", allowableValues = {"BROUILLON", "PUBLIE", "ARCHIVE"}) String statut
) {
    public static TypeMetierResponse depuis(TypeMetier t) {
        return new TypeMetierResponse(
                t.id(), t.tenantId(), t.businessDomainId(),
                t.code(), t.nom(), t.statut().name()
        );
    }
}
