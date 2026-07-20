package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.adapter.in.rest.access.CleApiCreeeResponse;
import com.yowyob.businesscore.adapter.in.rest.access.CleApiResponse;
import com.yowyob.businesscore.adapter.in.rest.access.CreerCleRequest;
import com.yowyob.businesscore.adapter.in.rest.access.RenommerCleRequest;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Clé API d'une application — exclusivement JWT (une clé API ne peut jamais gérer d'autres clés,
 * cf. {@code SecurityConfig}). Une application porte au plus une clé active à la fois : ces routes
 * opèrent donc directement sur « la » clé de l'application, sans identifiant de clé dans l'URL.
 */
@Tag(name = "Applications", description = "Clé API d'une application (au plus une active)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/applications/{businessId}")
public class BusinessApiKeyController {

    private final ApiKeyService apiKeyService;
    private final EntrepriseService entrepriseService;
    private final ResoudreDeveloppeurCourant developpeurCourant;

    public BusinessApiKeyController(ApiKeyService apiKeyService, EntrepriseService entrepriseService,
                                    ResoudreDeveloppeurCourant developpeurCourant) {
        this.apiKeyService = apiKeyService;
        this.entrepriseService = entrepriseService;
        this.developpeurCourant = developpeurCourant;
    }

    @Operation(summary = "Créer la clé API de cette application",
            description = "Émet le secret (`X-BC-Api-Key`). Le secret n'est affiché qu'une fois. "
                    + "Échoue si une clé est déjà active (révoquez-la d'abord).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Clé créée (secret inclus)"),
            @ApiResponse(responseCode = "404", description = "Application introuvable"),
            @ApiResponse(responseCode = "409", description = "Une clé est déjà active pour cette application")
    })
    @PostMapping("/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CleApiCreeeResponse> creer(@Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId,
                                           @RequestBody(required = false) CreerCleRequest requete) {
        String nom = requete == null ? null : requete.name();
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .then(developpeurCourant.id())
                .flatMap(developerId -> apiKeyService.creer(developerId, businessId, nom))
                .map(CleApiCreeeResponse::depuis);
    }

    @Operation(summary = "Consulter la clé active de cette application",
            description = "Métadonnées sans le secret (le secret n'est jamais consultable après sa création).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La clé active"),
            @ApiResponse(responseCode = "404", description = "Aucune clé active, ou application introuvable")
    })
    @GetMapping("/api-keys")
    public Mono<CleApiResponse> trouver(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .then(apiKeyService.trouverActive(businessId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Aucune clé active pour cette application : " + businessId)))
                .map(CleApiResponse::depuis);
    }

    @Operation(summary = "Renommer la clé active de cette application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clé renommée"),
            @ApiResponse(responseCode = "404", description = "Aucune clé active, ou application introuvable")
    })
    @PatchMapping("/api-keys")
    public Mono<CleApiResponse> renommer(@PathVariable UUID businessId,
                                         @Valid @RequestBody RenommerCleRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .then(apiKeyService.renommer(businessId, requete.name()))
                .map(CleApiResponse::depuis);
    }

    @Operation(summary = "Révoquer la clé active de cette application",
            description = "Révocation immédiate ; la clé ne peut plus s'authentifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clé révoquée"),
            @ApiResponse(responseCode = "404", description = "Aucune clé active, ou application introuvable")
    })
    @PostMapping("/api-keys:revoke")
    public Mono<CleApiResponse> revoquer(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .then(apiKeyService.revoquer(businessId))
                .map(CleApiResponse::depuis);
    }
}
