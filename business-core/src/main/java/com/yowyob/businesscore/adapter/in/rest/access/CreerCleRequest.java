package com.yowyob.businesscore.adapter.in.rest.access;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Création d'une clé API (nom optionnel)")
public record CreerCleRequest(
        @Schema(description = "Libellé libre", example = "Prod")
        String name
) {
}
