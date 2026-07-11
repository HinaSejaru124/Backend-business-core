package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService.DashboardData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Tableau de bord développeur — statistiques agrégées, aucune clé secrète")
public record DashboardResponse(
        @Schema(description = "Requêtes sur le mois glissant", example = "1240") long requetesCeMois,
        @Schema(example = "87") long requetesAujourdhui,
        @Schema(example = "3") long erreursAujourdhui,
        @Schema(description = "Taux d'erreur (0-1)", example = "0.034") double tauxErreur,
        List<PointJour> sparkline,
        @Schema(description = "Nombre d'entreprises du tenant", example = "3") long nombreEntreprises,
        @Schema(description = "Nombre de clés API actives, tous business confondus", example = "5")
        long nombreClesActives
) {

    @Schema(description = "Point de la courbe d'usage")
    public record PointJour(
            LocalDate jour,
            @Schema(example = "42") long total
    ) {
    }

    public static DashboardResponse depuis(DashboardData data) {
        List<PointJour> sparkline = data.sparkline().stream()
                .map(p -> new PointJour(p.jour(), p.total()))
                .toList();
        return new DashboardResponse(
                data.requetesCeMois(),
                data.requetesAujourdhui(),
                data.erreursAujourdhui(),
                data.tauxErreur(),
                sparkline,
                data.nombreEntreprises(),
                data.nombreClesActives());
    }
}
