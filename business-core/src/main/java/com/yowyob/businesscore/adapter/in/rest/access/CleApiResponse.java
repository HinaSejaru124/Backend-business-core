package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Vue d'une clé API (liste / renommage). Le secret n'y figure jamais : seul le {@code prefix} public
 * est exposé après la création.
 */
public record CleApiResponse(UUID id, String prefix, String name, String status,
                             Instant createdAt, Instant lastUsedAt) {

    public static CleApiResponse depuis(ApiKeyEntity e) {
        return new CleApiResponse(e.getId(), e.getPrefix(), e.getName(), e.getStatus(),
                e.getCreatedAt(), e.getLastUsedAt());
    }
}
