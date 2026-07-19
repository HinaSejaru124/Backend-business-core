package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vue d'une clé API (sans secret)")
public record CleApiResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(example = "Prod") String name,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "REVOKED"}) String status,
        Instant createdAt,
        Instant lastUsedAt,
        @Schema(description = "Application à laquelle cette clé est scopée") UUID entrepriseId
) {

    public static CleApiResponse depuis(ApiKeyEntity e) {
        return new CleApiResponse(e.getId(), e.getName(), e.getStatus(),
                e.getCreatedAt(), e.getLastUsedAt(), e.getEntrepriseId());
    }
}
