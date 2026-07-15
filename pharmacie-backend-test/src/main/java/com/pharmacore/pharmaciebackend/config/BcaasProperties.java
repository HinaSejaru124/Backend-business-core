package com.pharmacore.pharmaciebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration d'accès à Business Core (BCaaS). {@code clientId}/{@code apiKey} sont fournis
 * par variables d'environnement (jamais commités) — cf. .env.local.example.
 */
@ConfigurationProperties(prefix = "pharmacore.bcaas")
public record BcaasProperties(
        String baseUrl,
        String clientId,
        String apiKey,
        String typeId,
        int versionNumber,
        /**
         * Identifiant de l'entreprise Business Core que PharmaCore représente. La caisse (clé API) le
         * résout dynamiquement via {@code GET /v1/businesses/me} ; l'espace admin (JWT), lui, n'a pas de
         * « me » (un développeur gère plusieurs entreprises), d'où cette valeur explicite pour cibler
         * la bonne entreprise lors de la modélisation.
         */
        String businessId
) {
}
