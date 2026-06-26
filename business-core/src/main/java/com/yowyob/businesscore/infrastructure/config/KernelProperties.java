package com.yowyob.businesscore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'accès au kernel RT-Comops.
 *
 * <p>{@code baseUrl} : en dev/test, un mock (WireMock) ; en prod sur le réseau yowyob, l'hôte interne
 * {@code http://kernel-core-kernel-layer-1:8080} (et non l'URL publique). Changer de cible = changer
 * une propriété (variable d'env {@code KERNEL_BASE_URL}).
 *
 * <p>{@code clientId}/{@code clientSecret} : la <b>ClientApplication plateforme</b> du Business Core
 * (variables {@code KERNEL_CLIENT_ID}/{@code KERNEL_CLIENT_SECRET}). Elle sert à provisionner les
 * ClientApplications dédiées des développeurs ({@code POST /api/client-applications}) — appel qui
 * exige lui-même une ClientApplication valide via {@code X-Client-Id}/{@code X-Api-Key}.
 *
 * <p>{@code organizationService}/{@code businessActorRole} : valeurs exigées par le kernel lors de
 * l'auto-provisionnement d'une organisation (cf. {@code PersisterEntrepriseKernelAdapter}). Le
 * {@code service} doit correspondre à un code du catalogue de services kernel
 * ({@code GET /api/organizations/services/catalog}) ; le rôle est celui du business actor propriétaire
 * (OWNER). À ajuster par variable d'environnement selon l'environnement kernel cible.
 */
@ConfigurationProperties(prefix = "businesscore.kernel")
public record KernelProperties(
        String baseUrl,
        long timeoutMs,
        int maxRetries,
        String clientId,
        String clientSecret,
        String organizationService,
        String businessActorRole
) {

    public KernelProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8089";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
        if (maxRetries < 0) {
            maxRetries = 0;
        }
        clientId = clientId == null ? "" : clientId;
        clientSecret = clientSecret == null ? "" : clientSecret;
        organizationService = (organizationService == null || organizationService.isBlank())
                ? "BUSINESS_CORE" : organizationService;
        businessActorRole = (businessActorRole == null || businessActorRole.isBlank())
                ? "OWNER" : businessActorRole;
    }

    public boolean aDesCredentialsPlateforme() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }
}
