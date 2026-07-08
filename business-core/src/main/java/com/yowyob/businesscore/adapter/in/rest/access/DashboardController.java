package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Accès", description = "Inscription et gestion des clés d'API")
@RestController
@RequestMapping("/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DeveloperAccountRepository developerRepository;

    public DashboardController(DashboardService dashboardService,
                              DeveloperAccountRepository developerRepository) {
        this.dashboardService = dashboardService;
        this.developerRepository = developerRepository;
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
        return developerCourant()
                .flatMap(dashboardService::pour)
                .map(DashboardResponse::depuis);
    }

    private Mono<UUID> developerCourant() {
        return BusinessContextHolder.currentContext()
                .switchIfEmpty(Mono.error(ProblemException.forbidden("Contexte d'authentification absent")))
                .map(ctx -> ctx.tenantId())
                .flatMap(developerRepository::findByKernelTenantId)
                .map(account -> account.getId())
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable")));
    }
}
