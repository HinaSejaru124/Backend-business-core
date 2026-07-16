package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Rôle métier déclaré sur une version")
public record RoleReponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        UUID versionTypeId,
        @Schema(example = "CAISSIER") String code,
        @Schema(example = "OPERATEUR") CategorieActeur categorie
) {

    public static RoleReponse de(RoleMetier r) {
        return new RoleReponse(r.id(), r.versionTypeId(), r.code(), r.categorie());
    }
}
