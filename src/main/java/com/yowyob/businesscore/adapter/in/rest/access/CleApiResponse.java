package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vue d'une clé API")
public record CleApiResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "Prod") String name,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "REVOKED"}) String status,
        Instant createdAt,
        Instant lastUsedAt,
        @Schema(description = "Application à laquelle cette clé est scopée") UUID entrepriseId,
        @Schema(description = "Secret complet, visible par le propriétaire ; null pour les clés créées "
                + "avant l'activation de cette fonctionnalité") String apiKey
) {

    public static CleApiResponse depuis(ApiKeyEntity e) {
        return depuis(e, null);
    }

    public static CleApiResponse depuis(ApiKeyEntity e, String secretClair) {
        return new CleApiResponse(e.getId(), e.getName(), e.getStatus(),
                e.getCreatedAt(), e.getLastUsedAt(), e.getEntrepriseId(), secretClair);
    }
}
