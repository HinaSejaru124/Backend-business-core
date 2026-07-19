package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Acteur métier rattaché à une application")
public record ActeurReponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        UUID entrepriseId,
        @Schema(description = "Rôle métier assigné") UUID roleMetierId,
        @Schema(description = "Identifiant acteur côté kernel") UUID acteurKernelId,
        Instant valideDepuis,
        Instant valideJusqua
) {

    public static ActeurReponse de(ActeurMetier a) {
        return new ActeurReponse(a.id(), a.entrepriseId(), a.roleMetierId(),
                a.acteurKernelId(), a.valideDepuis(), a.valideJusqua());
    }
}
