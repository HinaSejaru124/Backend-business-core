package com.yowyob.businesscore.adapter.in.rest.access;

/** Réponse de POST /v1/registration. {@code apiKey} est le secret, affiché une seule fois. */
public record ApiKeyResponse(String clientId, String apiKey, String plan) {
}
