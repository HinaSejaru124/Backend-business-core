package com.pharmacore.pharmaciebackend.dashboard;

import java.math.BigDecimal;

/**
 * chiffreAffairesDuJour/nombreVentesDuJour sont calculés sur les {@code vente} réellement persistées
 * (POST /api/ventes) depuis minuit UTC. Une vente n'est persistée que si Business Core confirme le
 * résultat de {@code Vendre:execute} — tant qu'ENGAGER_STOCK reste bloqué côté Kernel (cf.
 * FEUILLE-DE-ROUTE.md §8), ces valeurs restent à zéro parce qu'aucune vente n'aboutit, pas parce
 * qu'elles sont figées en dur.
 */
public record DashboardResponse(
        long totalMedicaments,
        long alertesStockActives,
        BigDecimal chiffreAffairesDuJour,
        long nombreVentesDuJour
) {
}
