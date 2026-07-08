package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService.DashboardData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Tableau de bord développeur (30 jours)")
public record DashboardResponse(
        @Schema(description = "Requêtes sur le mois glissant", example = "1240") long requetesCeMois,
        @Schema(example = "87") long requetesAujourdhui,
        @Schema(example = "3") long erreursAujourdhui,
        @Schema(description = "Taux d'erreur (0-1)", example = "0.034") double tauxErreur,
        List<PointJour> sparkline,
        List<CleApiResponse> cles
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
        List<CleApiResponse> cles = data.cles().stream()
                .map(CleApiResponse::depuis)
                .toList();
        return new DashboardResponse(
                data.requetesCeMois(),
                data.requetesAujourdhui(),
                data.erreursAujourdhui(),
                data.tauxErreur(),
                sparkline,
                cles);
    }
}
