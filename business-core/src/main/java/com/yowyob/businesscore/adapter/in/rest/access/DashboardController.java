package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Tableau de bord développeur — {@code GET /v1/dashboard}. Synthèse d'usage (30 jours) et clés API du
 * développeur courant (résolu depuis le tenant kernel du {@link BusinessContext}).
 */
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

    @GetMapping
    public Mono<DashboardResponse> tableau() {
        return developerCourant()
                .flatMap(dashboardService::pour)
                .map(DashboardResponse::depuis);
    }

    private Mono<UUID> developerCourant() {
        return BusinessContextHolder.currentContext()
                .switchIfEmpty(Mono.error(ProblemException.forbidden("Contexte d'authentification absent")))
                .map(BusinessContext::tenantId)
                .flatMap(developerRepository::findByKernelTenantId)
                .map(DeveloperAccountEntity::getId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable")));
    }
}
