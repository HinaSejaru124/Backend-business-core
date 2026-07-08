package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService.DashboardData;

import java.time.LocalDate;
import java.util.List;

/**
 * Réponse de {@code GET /v1/dashboard} : synthèse d'usage du développeur et liste de ses clés API.
 */
public record DashboardResponse(
        long requetesCeMois,
        long requetesAujourdhui,
        long erreursAujourdhui,
        double tauxErreur,
        List<PointJour> sparkline,
        List<CleApiResponse> cles) {

    public record PointJour(LocalDate jour, long total) {
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
