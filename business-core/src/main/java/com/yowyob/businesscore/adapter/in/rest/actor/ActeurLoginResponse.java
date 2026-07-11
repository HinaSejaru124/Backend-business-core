package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.usecase.actor.AuthentifierActeurService.ActeurConnecte;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Connexion acteur — JWT kernel + contexte métier résolu (entreprise, rôle)")
public record ActeurLoginResponse(
        @Schema(description = "JWT kernel, à rejouer en Bearer") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "3600") long expiresInSeconds,
        @Schema(description = "Acteur métier résolu dans cette entreprise") ActeurReponse acteur,
        @Schema(description = "Code du rôle métier actif", example = "PHARMACIEN") String roleCode
) {
    public static ActeurLoginResponse depuis(ActeurConnecte c) {
        return new ActeurLoginResponse(
                c.session().accessToken(),
                "Bearer",
                c.session().expiresInSeconds(),
                ActeurReponse.de(c.acteur()),
                c.role().code());
    }
}
