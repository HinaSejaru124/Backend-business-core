package com.yowyob.businesscore.adapter.in.rest.billing;

import com.yowyob.businesscore.application.billing.PlanCatalogue;
import com.yowyob.businesscore.application.billing.PlanService;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Catalogue des plans et changement de plan (facturation).
 *
 * <p>Le changement de plan passe par le port de paiement : aujourd'hui un adapter de simulation confirme
 * immédiatement (l'API de paiement Kernel Core n'est pas encore disponible), demain le vrai adapter
 * Kernel. Chaque développeur n'agit que sur SON compte (résolu depuis le JWT).
 */
@Tag(name = "Facturation", description = "Plans tarifaires et quotas")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1")
public class PlanController {

    private final PlanCatalogue catalogue;
    private final PlanService planService;
    private final ResoudreDeveloppeurCourant developpeurCourant;

    public PlanController(PlanCatalogue catalogue,
                          PlanService planService,
                          ResoudreDeveloppeurCourant developpeurCourant) {
        this.catalogue = catalogue;
        this.planService = planService;
        this.developpeurCourant = developpeurCourant;
    }

    @Operation(summary = "Catalogue des plans", description = "Liste les plans et leurs quotas mensuels.")
    @ApiResponse(responseCode = "200", description = "Catalogue des plans")
    @GetMapping("/plans")
    public Mono<List<PlanResponse>> plans() {
        return Mono.just(catalogue.plans().entrySet().stream()
                .map(e -> PlanResponse.depuis(e.getKey(), e.getValue()))
                .toList());
    }

    @Operation(summary = "Changer de plan",
            description = "Demande le passage vers le plan cible (paiement via Kernel Core, simulé pour l'instant).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan changé (CONFIRME) ou paiement en attente"),
            @ApiResponse(responseCode = "400", description = "Plan cible invalide"),
            @ApiResponse(responseCode = "409", description = "Déjà sur ce plan"),
            @ApiResponse(responseCode = "422", description = "Paiement refusé")
    })
    @PostMapping("/plan/upgrade")
    public Mono<UpgradeResponse> upgrade(@Valid @RequestBody UpgradePlanRequest requete) {
        return developpeurCourant.id()
                .flatMap(developerId -> planService.changer(developerId, requete.targetPlan()))
                .map(UpgradeResponse::depuis);
    }
}
