package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService.DashboardData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Tableau de bord développeur — statistiques agrégées, aucune clé secrète")
public record DashboardResponse(
        @Schema(description = "Requêtes sur le mois glissant", example = "1240") long requetesCeMois,
        @Schema(example = "87") long requetesAujourdhui,
        @Schema(example = "3") long erreursAujourdhui,
        @Schema(description = "Taux d'erreur (0-1)", example = "0.034") double tauxErreur,
        List<PointJour> sparkline,
        @Schema(description = "Nombre d'entreprises du tenant", example = "3") long nombreEntreprises,
        @Schema(description = "Nombre de clés API actives, tous business confondus", example = "5")
        long nombreClesActives,
        @Schema(description = "Opérations les plus exécutées sur 30 jours") List<TopOperation> topOperations,
        @Schema(description = "Entreprises les plus actives sur 30 jours") List<TopEntreprise> topEntreprises,
        @Schema(description = "Dernières exécutions d'opération, toutes entreprises confondues")
        List<ActiviteItem> activiteRecente
) {

    @Schema(description = "Point de la courbe d'usage")
    public record PointJour(
            LocalDate jour,
            @Schema(example = "42") long total
    ) {
    }

    @Schema(description = "Opération et son nombre d'exécutions")
    public record TopOperation(
            @Schema(example = "vente") String nom,
            @Schema(example = "1200") long total
    ) {
    }

    @Schema(description = "Entreprise et son nombre d'exécutions")
    public record TopEntreprise(
            UUID entrepriseId,
            @Schema(example = "Pharmacie Centrale") String nom,
            @Schema(example = "3000") long total
    ) {
    }

    @Schema(description = "Élément de l'activité récente")
    public record ActiviteItem(
            UUID entrepriseId,
            @Schema(example = "Pharmacie Centrale") String entrepriseNom,
            @Schema(example = "vente") String operationNom,
            @Schema(example = "COMPLETEE") String statut,
            Instant creeLe
    ) {
    }

    public static DashboardResponse depuis(DashboardData data) {
        List<PointJour> sparkline = data.sparkline().stream()
                .map(p -> new PointJour(p.jour(), p.total()))
                .toList();
        List<TopOperation> topOperations = data.topOperations().stream()
                .map(t -> new TopOperation(t.nom(), t.total()))
                .toList();
        List<TopEntreprise> topEntreprises = data.topEntreprises().stream()
                .map(t -> new TopEntreprise(t.entrepriseId(), t.nom(), t.total()))
                .toList();
        List<ActiviteItem> activiteRecente = data.activiteRecente().stream()
                .map(a -> new ActiviteItem(a.entrepriseId(), a.entrepriseNom(), a.operationNom(), a.statut(), a.creeLe()))
                .toList();
        return new DashboardResponse(
                data.requetesCeMois(),
                data.requetesAujourdhui(),
                data.erreursAujourdhui(),
                data.tauxErreur(),
                sparkline,
                data.nombreEntreprises(),
                data.nombreClesActives(),
                topOperations,
                topEntreprises,
                activiteRecente);
    }
}
