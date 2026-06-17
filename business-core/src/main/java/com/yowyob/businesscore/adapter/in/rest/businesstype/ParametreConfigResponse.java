package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.domain.configuration.ParametreConfig;

import java.util.UUID;

/** Réponse JSON pour un paramètre de configuration (schéma OpenAPI {@code ConfigParam}). */
public record ParametreConfigResponse(
        UUID    id,
        String  cle,
        String  valeur,
        boolean verrouille,
        String  portee          // TYPE | ENTREPRISE
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
