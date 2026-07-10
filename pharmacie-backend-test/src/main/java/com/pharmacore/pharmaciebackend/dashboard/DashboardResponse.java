package com.pharmacore.pharmaciebackend.dashboard;

import java.math.BigDecimal;

/**
 * chiffreAffairesDuJour/nombreVentesDuJour restent à zéro tant que l'endpoint /api/ventes n'existe
 * pas (bloqué par l'entreprise BCaaS, cf. SPIKE-RESULTATS.md) — valeur réelle, pas simulée : il n'y a
 * simplement aucune vente possible pour l'instant.
 */
public record DashboardResponse(
        long totalMedicaments,
        long alertesStockActives,
        BigDecimal chiffreAffairesDuJour,
        long nombreVentesDuJour
) {
}
