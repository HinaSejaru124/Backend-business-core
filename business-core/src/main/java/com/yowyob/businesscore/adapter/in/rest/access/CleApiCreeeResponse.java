package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.ApiKeyService.CleApiCreee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Clé API créée — secret affiché une seule fois")
public record CleApiCreeeResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(description = "Client-Id (`X-BC-Client-Id`)", example = "bc_live_abc123") String clientId,
        @Schema(description = "Secret (`X-BC-Api-Key`)") String apiKey,
        @Schema(example = "Prod") String name
) {

    public static CleApiCreeeResponse depuis(CleApiCreee cle) {
        return new CleApiCreeeResponse(cle.id(), cle.prefix(), cle.secret(), cle.name());
    }
}
