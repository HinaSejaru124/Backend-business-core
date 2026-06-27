package com.yowyob.businesscore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'authentification déléguée au kernel (cf. {@code Guide_Special_Auth.md}).
 *
 * <ul>
 *   <li>{@code tenantId} : tenant kernel utilisé en mode mono-tenant (en-tête {@code X-Tenant-Id}
 *       au login et sur les appels). En multi-tenant (plus tard), il se découvrira via
 *       {@code discover-contexts} et ce défaut ne servira que de repli.</li>
 *   <li>{@code issuer} : émetteur attendu du JWT ({@code iss}). Si vide, la vérification de l'émetteur
 *       est ignorée (utile en dev/test).</li>
 *   <li>{@code jwksUri} : URI absolue des clés publiques RS256 du kernel. Si vide, on la déduit de
 *       {@link KernelProperties#baseUrl()} + {@code /.well-known/jwks.json}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "businesscore.auth")
public record AuthProperties(
        String tenantId,
        String issuer,
        String jwksUri
) {

    public AuthProperties {
        tenantId = tenantId == null ? "" : tenantId.trim();
        issuer = issuer == null ? "" : issuer.trim();
        jwksUri = jwksUri == null ? "" : jwksUri.trim();
    }

    public boolean aUnTenantParDefaut() {
        return !tenantId.isBlank();
    }

    public boolean verifieEmetteur() {
        return !issuer.isBlank();
    }

    /** URI JWKS effective : celle configurée, sinon déduite de la base-url kernel. */
    public String jwksUriEffective(String kernelBaseUrl) {
        if (!jwksUri.isBlank()) {
            return jwksUri;
        }
        String base = kernelBaseUrl == null ? "" : kernelBaseUrl.replaceAll("/+$", "");
        return base + "/.well-known/jwks.json";
    }
}
