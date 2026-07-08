package com.yowyob.businesscore.adapter.in.rest.access;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse d'inscription — clé BC émise une seule fois")
public record ApiKeyResponse(
        @Schema(description = "Client-Id (`X-BC-Client-Id`)", example = "bc_live_abc123")
        String clientId,
        @Schema(description = "Secret (`X-BC-Api-Key`) — affiché une seule fois") String apiKey,
        @Schema(example = "FREE") String plan
) {
}
