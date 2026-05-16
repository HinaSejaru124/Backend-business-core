package com.bcaas.core.tenant.domain.model;

/**
 * Configuration personnalisable d'un tenant.
 * Permet de paramétrer le système sans redéployer (control plane).
 * Analogie réseau : table de routage personnalisée par VLAN.
 */
public record TenantSettings(
        String defaultLocale,
        String defaultCurrency,
        String timezone,
        boolean notificationsEnabled,
        boolean auditEnabled,
        int sessionTimeoutMinutes
) {
    public static TenantSettings defaults() {
        return new TenantSettings(
                "fr",
                "XAF",
                "Africa/Douala",
                true,
                true,
                60
        );
    }

    public TenantSettings withLocale(String locale) {
        return new TenantSettings(
                locale,
                this.defaultCurrency,
                this.timezone,
                this.notificationsEnabled,
                this.auditEnabled,
                this.sessionTimeoutMinutes
        );
    }

    public TenantSettings withCurrency(String currency) {
        return new TenantSettings(
                this.defaultLocale,
                currency,
                this.timezone,
                this.notificationsEnabled,
                this.auditEnabled,
                this.sessionTimeoutMinutes
        );
    }
}
