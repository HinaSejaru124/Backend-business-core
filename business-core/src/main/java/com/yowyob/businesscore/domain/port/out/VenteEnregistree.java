package com.yowyob.businesscore.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Résultat d'un enregistrement de vente.
 *
 * @param commandeId          identifiant de la commande de vente (order) — sert de point de
 *                            compensation ({@code POST /api/sales/orders/{id}/cancel}).
 * @param transactionKernelId identifiant de la facture (cashier bill) produite.
 * @param montant             montant facturé.
 * @param devise              devise du montant.
 */
public record VenteEnregistree(UUID commandeId, UUID transactionKernelId, BigDecimal montant, String devise) {
}
