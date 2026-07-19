package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.ApiKeyService.CleApiCreee;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Clé API créée — secret affiché une seule fois")
public record CleApiCreeeResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(description = "Secret (`X-BC-Api-Key`). Le `X-BC-Client-Id` associé est votre "
                + "identifiant développeur stable, voir GET /v1/auth/me — il ne change pas d'une clé à l'autre.")
        String apiKey,
        @Schema(example = "Prod") String name,
        @Schema(description = "Application à laquelle cette clé est scopée") UUID entrepriseId
) {

    public static CleApiCreeeResponse depuis(CleApiCreee cle) {
        return new CleApiCreeeResponse(cle.id(), cle.secret(), cle.name(), cle.entrepriseId());
    }
}
