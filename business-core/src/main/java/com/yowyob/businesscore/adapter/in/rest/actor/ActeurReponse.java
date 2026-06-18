package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;

import java.time.Instant;
import java.util.UUID;

/** Réponse d'un acteur métier rattaché (aligné OpenAPI {@code Actor}). */
public record ActeurReponse(
        UUID id,
        UUID entrepriseId,
        UUID roleMetierId,
        UUID acteurKernelId,
        Instant valideDepuis,
        Instant valideJusqua) {

    public static ActeurReponse de(ActeurMetier a) {
        return new ActeurReponse(a.id(), a.entrepriseId(), a.roleMetierId(),
                a.acteurKernelId(), a.valideDepuis(), a.valideJusqua());
    }
}
