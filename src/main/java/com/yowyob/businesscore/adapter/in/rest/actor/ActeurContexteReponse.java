package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.usecase.actor.AuthentifierActeurService.ActeurConnecte;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = """
        Contexte métier de l'acteur courant dans cette entreprise — KCore a répondu « qui es-tu » (le JWT),
        ceci répond « que peux-tu faire dans ce Business ».
        """)
public record ActeurContexteReponse(
        @Schema(description = "Acteur métier résolu dans cette entreprise") ActeurReponse acteur,
        @Schema(description = "Code du rôle métier actif", example = "PHARMACIEN") String roleCode,
        @Schema(description = "Permissions techniques portées par le JWT kernel")
        Set<String> permissions
) {
    public static ActeurContexteReponse depuis(ActeurConnecte c, Set<String> permissions) {
        return new ActeurContexteReponse(ActeurReponse.de(c.acteur()), c.role().code(), permissions);
    }
}
