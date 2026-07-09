package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.DashboardService;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Accès", description = "Inscription et gestion des clés d'API")
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
            description = "Synthèse d'usage sur 30 jours et état des clés API du développeur courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Données du tableau de bord"),
            @ApiResponse(responseCode = "403", description = "Contexte développeur absent"),
            @ApiResponse(responseCode = "404", description = "Compte développeur introuvable")
    })
    @GetMapping
    public Mono<DashboardResponse> tableau() {
        return developpeurCourant.id()
                .flatMap(dashboardService::pour)
                .map(DashboardResponse::depuis);
    }
}
