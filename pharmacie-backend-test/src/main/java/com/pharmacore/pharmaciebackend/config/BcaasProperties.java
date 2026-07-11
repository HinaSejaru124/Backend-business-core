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
        int versionNumber
) {
}
