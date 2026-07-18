package com.yowyob.businesscore.adapter.out.payment;

/**
 * Vue de l'ordre de paiement renvoyé par la passerelle Kernel Core
 * ({@code POST /api/payments/orders} et {@code .../{id}/refresh}).
 *
 * <p>Le {@code KernelClient} déballe déjà l'enveloppe {@code {success,data,…}} : on type donc sur le
 * contenu {@code data}. Seuls les champs utiles au parcours d'upgrade sont déclarés ; les propriétés
 * inconnues sont ignorées (mapper Jackson non strict, convention du socle).
 */
public record PaymentOrderResponse(
        String id,
        String tenantId,
        String status,
        String providerReference,
        String redirectUrl,
        Double amount,
        String currency
) {
}
