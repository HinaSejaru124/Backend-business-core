package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmation d'inscription — aucune clé API n'est émise ici")
public record InscriptionResponse(
        @Schema(example = "FREE") String plan,
        String message,
        @Schema(description = "Identifiant à utiliser pour se connecter. La plateforme d'identité génère "
                + "une adresse @yowyob.com : l'adresse personnelle saisie à l'inscription sert à la "
                + "vérification et à la récupération, mais ne permet pas de se connecter.",
                example = "jeandupont1a2b3@yowyob.com")
        String identifiantConnexion
) {
    public static InscriptionResponse depuis(ApiKeyEmise emise) {
        return new InscriptionResponse(emise.plan(), emise.message(), emise.identifiantConnexion());
    }
}
