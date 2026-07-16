package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.access.DashboardService;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Tableau de bord développeur — exclusivement JWT (cf. {@code SecurityConfig}). Statistiques agrégées
 * et publiques uniquement (nombre d'entreprises, de clés actives, usage) : jamais de secret ni de
 * détail de clé individuelle, voir {@code /v1/businesses/{id}/api-keys} pour la gestion d'une clé.
 */
@Tag(name = "Accès", description = "Tableau de bord développeur")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ResoudreDeveloppeurCourant developpeurCourant;

    public DashboardController(DashboardService dashboardService,
                              ResoudreDeveloppeurCourant developpeurCourant) {
        this.dashboardService = dashboardService;
        this.developpeurCourant = developpeurCourant;
    }

    @Operation(summary = "Tableau de bord développeur",
            description = "Synthèse d'usage sur 30 jours et compteurs publics (entreprises, clés actives).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Données du tableau de bord"),
            @ApiResponse(responseCode = "404", description = "Compte développeur introuvable")
    })
    @GetMapping
    public Mono<DashboardResponse> tableau() {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> developpeurCourant.id()
                        .flatMap(developerId -> dashboardService.pour(developerId, ctx.tenantId())))
                .map(DashboardResponse::depuis);
    }

    @Operation(summary = "Courbe d'usage sur une fenêtre choisie",
            description = "Sélecteur de période du graphique de consommation (1/7/30/365 jours). "
                    + "Granularité réelle journalière — n'affecte jamais le calcul du quota mensuel, "
                    + "toujours basé sur le mois calendaire réel (voir GET /v1/dashboard).")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Points de la courbe")})
    @GetMapping("/sparkline")
    public Mono<List<DashboardResponse.PointJour>> sparkline(
            @RequestParam(defaultValue = "30") int jours) {
        int fenetre = Math.min(Math.max(jours, 1), 366);
        return developpeurCourant.id()
                .flatMap(developerId -> dashboardService.sparklinePour(developerId, fenetre))
                .map(points -> points.stream()
                        .map(p -> new DashboardResponse.PointJour(p.jour(), p.total()))
                        .toList());
    }
}
