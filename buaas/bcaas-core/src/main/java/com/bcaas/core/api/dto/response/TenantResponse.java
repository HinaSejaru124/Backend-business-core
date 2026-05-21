package com.bcaas.core.api.dto.response;

import com.bcaas.core.tenant.domain.model.Tenant;
import java.time.Instant;

/**
 * DTO de réponse Tenant.
 * Expose uniquement les données nécessaires au client.
 * Ne jamais exposer les agrégats du domaine directement.
 */
public record TenantResponse(
        String id,
        String name,
        String slug,
        String status,
        String plan,
        String defaultLocale,
        String defaultCurrency,
        String timezone,
        Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus().name(),
                tenant.getPlan().name(),
                tenant.getSettings().defaultLocale(),
                tenant.getSettings().defaultCurrency(),
                tenant.getSettings().timezone(),
                tenant.getAuditInfo().createdAt()
        );
    }
}
