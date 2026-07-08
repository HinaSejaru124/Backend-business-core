package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.ApiKeyService.CleApiCreee;

import java.util.UUID;

/**
 * Réponse de création d'une clé API. {@code apiKey} (le secret) n'est renvoyé qu'ici, une seule fois :
 * il n'est jamais re-consultable ensuite (seul le {@code clientId}/prefix reste visible).
 */
public record CleApiCreeeResponse(UUID id, String clientId, String apiKey, String name) {

    public static CleApiCreeeResponse depuis(CleApiCreee cle) {
        return new CleApiCreeeResponse(cle.id(), cle.prefix(), cle.secret(), cle.name());
    }
}
