package com.yowyob.businesscore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de la passerelle de paiement Kernel Core (mobile money via MyCoolPay), utilisés par
 * {@code KernelPaiementAdapter} pour ouvrir un ordre de paiement d'upgrade de plan.
 *
 * <p>Configuré via {@code businesscore.payment.*}. {@code callbackBaseUrl} est l'URL de retour côté
 * console où le développeur est renvoyé après le paiement (la page ajoute {@code ?payment=<id>} pour
 * déclencher la finalisation). Les valeurs par défaut ciblent l'environnement mobile money réel
 * (MyCoolPay / MOBILE_MONEY / XAF) ; {@code clientId} doit correspondre à la ClientApplication plateforme
 * du Business Core côté kernel.
 */
@ConfigurationProperties(prefix = "businesscore.payment")
public record PaymentProperties(
        String clientId,
        String provider,
        String method,
        String currency,
        String serviceCodePrefix,
        String callbackBaseUrl
) {

    public PaymentProperties {
        if (clientId == null || clientId.isBlank()) {
            clientId = "business-core";
        }
        if (provider == null || provider.isBlank()) {
            provider = "MYCOOLPAY";
        }
        if (method == null || method.isBlank()) {
            method = "MOBILE_MONEY";
        }
        if (currency == null || currency.isBlank()) {
            currency = "XAF";
        }
        if (serviceCodePrefix == null) {
            serviceCodePrefix = "PLAN_UPGRADE_";
        }
        if (callbackBaseUrl == null || callbackBaseUrl.isBlank()) {
            callbackBaseUrl = "http://localhost:3000/console/pricing";
        }
    }
}
