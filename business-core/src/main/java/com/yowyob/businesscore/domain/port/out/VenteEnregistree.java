package com.yowyob.businesscore.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Résultat d'un enregistrement de vente.
 *
 * <p>Une vente client suit la chaîne kernel {@code order → confirm → facture client → bill cashier}.
 * Le {@code billId} est l'identifiant du <b>bill cashier</b> produit : c'est lui qu'on encaisse
 * ({@code POST /api/bills/pay}) et c'est lui qui sert de référence de transaction kernel pour la trace.
 *
 * @param commandeId identifiant de la commande de vente (order) — point de compensation
 *                   ({@code POST /api/sales/orders/{id}/cancel}).
 * @param billId     identifiant du bill cashier à encaisser / référence de transaction kernel.
 * @param montant    montant facturé.
 * @param devise     devise du montant.
 */
public record VenteEnregistree(UUID commandeId, UUID billId, BigDecimal montant, String devise) {
}