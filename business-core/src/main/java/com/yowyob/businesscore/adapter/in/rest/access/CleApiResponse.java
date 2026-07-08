package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vue d'une clé API (sans secret)")
public record CleApiResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
        @Schema(description = "Préfixe public", example = "bc_live_abc123") String prefix,
        @Schema(example = "Prod") String name,
        @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "REVOKED"}) String status,
        Instant createdAt,
        Instant lastUsedAt
) {

    public static CleApiResponse depuis(ApiKeyEntity e) {
        return new CleApiResponse(e.getId(), e.getPrefix(), e.getName(), e.getStatus(),
                e.getCreatedAt(), e.getLastUsedAt());
    }
}
