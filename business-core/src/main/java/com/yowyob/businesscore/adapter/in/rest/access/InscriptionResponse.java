package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.domain.port.in.ApiKeyEmise;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmation d'inscription — aucune clé API n'est émise ici")
public record InscriptionResponse(
        @Schema(example = "FREE") String plan,
        String message
) {
    public static InscriptionResponse depuis(ApiKeyEmise emise) {
        return new InscriptionResponse(emise.plan(), emise.message());
    }
}
