package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService.DashboardData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Tableau de bord développeur — statistiques agrégées, aucune clé secrète")
public record DashboardResponse(
        @Schema(description = "Plan tarifaire courant", example = "FREE") String plan,
        @Schema(description = "Quota mensuel de requêtes (-1 = illimité)", example = "1000") long quotaMensuel,
        @Schema(description = "Requêtes restantes ce mois (-1 = illimité)", example = "913") long requetesRestantes,
        @Schema(description = "Quota mensuel atteint (compte bloqué en clé API)", example = "false") boolean bloque,
        @Schema(description = "Requêtes sur le mois calendaire courant", example = "1240") long requetesCeMois,
        @Schema(example = "87") long requetesAujourdhui,
        @Schema(example = "3") long erreursAujourdhui,
        @Schema(description = "Taux d'erreur (0-1)", example = "0.034") double tauxErreur,
        List<PointJour> sparkline,
        @Schema(description = "Nombre d'applications du tenant", example = "3") long nombreApplications,
        @Schema(description = "Nombre de clés API actives, toutes applications confondues", example = "5")
        long nombreClesActives,
        @Schema(description = "Temps de réponse moyen en ms sur 30 jours (null si aucune requête)", example = "128.4")
        Double tempsReponseMoyenMs,
        @Schema(description = "Opérations les plus exécutées sur 30 jours") List<TopOperation> topOperations,
        @Schema(description = "Applications les plus actives sur 30 jours") List<TopApplication> topApplications,
        @Schema(description = "Dernières exécutions d'opération, toutes applications confondues")
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

    @Schema(description = "Application et son nombre d'exécutions")
    public record TopApplication(
            UUID applicationId,
            @Schema(example = "Pharmacie Centrale") String nom,
            @Schema(example = "3000") long total
    ) {
    }

    @Schema(description = "Élément de l'activité récente")
    public record ActiviteItem(
            UUID applicationId,
            @Schema(example = "Pharmacie Centrale") String applicationNom,
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
        List<TopApplication> topApplications = data.topEntreprises().stream()
                .map(t -> new TopApplication(t.entrepriseId(), t.nom(), t.total()))
                .toList();
        List<ActiviteItem> activiteRecente = data.activiteRecente().stream()
                .map(a -> new ActiviteItem(a.entrepriseId(), a.entrepriseNom(), a.operationNom(), a.statut(), a.creeLe()))
                .toList();
        return new DashboardResponse(
                data.plan(),
                data.quotaMensuel(),
                data.requetesRestantes(),
                data.bloque(),
                data.requetesCeMois(),
                data.requetesAujourdhui(),
                data.erreursAujourdhui(),
                data.tauxErreur(),
                sparkline,
                data.nombreEntreprises(),
                data.nombreClesActives(),
                data.tempsReponseMoyenMs(),
                topOperations,
                topApplications,
                activiteRecente);
    }
}
