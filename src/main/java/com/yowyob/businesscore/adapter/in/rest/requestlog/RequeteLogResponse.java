package com.yowyob.businesscore.adapter.in.rest.requestlog;

import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogEntity;

import java.time.Instant;
import java.util.UUID;

/** Projection publique d'une ligne du journal détaillé des requêtes (onglet Audit / Requêtes). */
public record RequeteLogResponse(
        UUID id,
        String categorie,
        String methode,
        String endpoint,
        int statutHttp,
        long dureeMs,
        boolean facturable,
        Instant creeLe) {

    public static RequeteLogResponse depuis(RequeteLogEntity e) {
        return new RequeteLogResponse(
                e.getId(), e.getCategorie(), e.getMethode(), e.getEndpoint(),
                e.getStatutHttp(), e.getDureeMs() != null ? e.getDureeMs() : 0L,
                e.getFacturable() != null ? e.getFacturable() : true, e.getCreeLe());
    }
}
