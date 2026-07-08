package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.configuration.ParametreConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Paramètre de configuration")
public record ParametreConfigResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "devise") String cle,
        @Schema(example = "XOF") String valeur,
        boolean verrouille,
        @Schema(example = "TYPE", allowableValues = {"TYPE", "ENTREPRISE"}) String portee
) {
    public static ParametreConfigResponse depuis(ParametreConfig p) {
        return new ParametreConfigResponse(
                p.id(),
                p.cle(),
                p.valeur(),
                p.verrouille(),
                p.estNiveauType() ? "TYPE" : "ENTREPRISE"
        );
    }
}
