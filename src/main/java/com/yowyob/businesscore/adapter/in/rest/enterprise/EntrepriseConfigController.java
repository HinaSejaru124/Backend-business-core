package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.adapter.in.rest.businesstype.ParametreConfigResponse;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.configuration.ConfigurationService;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Configuration (brique 7) — niveau ENTREPRISE : surcharge locale des paramètres définis par défaut
 * sur la version de Type épinglée (voir {@code BusinessTypeController} pour le niveau TYPE).
 * Impossible de surcharger un paramètre verrouillé au Type (409 {@code PARAMETRE_VERROUILLE}).
 */
@Tag(name = "Configuration application", description = "Surcharge locale des paramètres de configuration (brique 7)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/v1/businesses/{businessId}/config", "/v1/applications/{businessId}/config"})
public class EntrepriseConfigController {

    private final EntrepriseService entrepriseService;
    private final ConfigurationService configService;

    public EntrepriseConfigController(EntrepriseService entrepriseService, ConfigurationService configService) {
        this.entrepriseService = entrepriseService;
        this.configService = configService;
    }

    @Operation(summary = "Lister la configuration surchargée de l'application",
            description = "Uniquement les surcharges locales ; le défaut du Type se consulte via "
                    + "/v1/business-types/{typeId}/versions/{n}/config.")
    @ApiResponse(responseCode = "200", description = "Liste des surcharges")
    @GetMapping
    public Flux<ParametreConfigResponse> lister(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .flatMapMany(entreprise -> configService.listerParEntreprise(entreprise.id()))
                .map(ParametreConfigResponse::depuis);
    }

    @Operation(summary = "Surcharger un paramètre de configuration",
            description = "Remplace la valeur effective pour cette application. Le paramètre doit déjà "
                    + "exister au niveau Type et ne pas y être verrouillé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paramètre surchargé"),
            @ApiResponse(responseCode = "404", description = "Application introuvable, ou paramètre absent au niveau Type"),
            @ApiResponse(responseCode = "409", description = "Paramètre verrouillé par le Type (PARAMETRE_VERROUILLE)")
    })
    @PutMapping("/{cle}")
    public Mono<ParametreConfigResponse> surcharger(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId,
            @Parameter(description = "Clé du paramètre") @PathVariable String cle,
            @Valid @RequestBody SurchargerParametreRequest req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx)
                        .flatMap(entreprise -> configService.surchargerpourEntreprise(
                                entreprise.id(), entreprise.versionTypeId(), cle, req.valeur(), ctx)))
                .map(ParametreConfigResponse::depuis);
    }
}
