package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import com.yowyob.businesscore.application.usecase.access.ResoudreDeveloppeurCourant;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Accès", description = "Inscription et gestion des clés d'API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final ResoudreDeveloppeurCourant developpeurCourant;

    public ApiKeyController(ApiKeyService apiKeyService,
                           ResoudreDeveloppeurCourant developpeurCourant) {
        this.apiKeyService = apiKeyService;
        this.developpeurCourant = developpeurCourant;
    }

    @Operation(summary = "Créer une clé API",
            description = "Émet une nouvelle paire Client-Id / Api-Key. Le secret n'est affiché qu'une fois.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Clé créée (secret inclus)"),
            @ApiResponse(responseCode = "403", description = "Contexte développeur absent")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CleApiCreeeResponse> creer(@RequestBody(required = false) CreerCleRequest requete) {
        String nom = requete == null ? null : requete.name();
        return developpeurCourant.id()
                .flatMap(developerId -> apiKeyService.creer(developerId, nom))
                .map(CleApiCreeeResponse::depuis);
    }

    @Operation(summary = "Lister ses clés API actives", description = "Retourne les clés ACTIVE sans le secret.")
    @ApiResponse(responseCode = "200", description = "Liste des clés")
    @GetMapping
    public Mono<List<CleApiResponse>> lister() {
        return developpeurCourant.id()
                .flatMap(apiKeyService::listerCollect)
                .map(cles -> cles.stream().map(CleApiResponse::depuis).toList());
    }

    @Operation(summary = "Renommer une clé API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clé renommée"),
            @ApiResponse(responseCode = "404", description = "Clé introuvable")
    })
    @PatchMapping("/{id}")
    public Mono<CleApiResponse> renommer(
            @Parameter(description = "Identifiant de la clé") @PathVariable UUID id,
            @Valid @RequestBody RenommerCleRequest requete) {
        return developpeurCourant.id()
                .flatMap(developerId -> apiKeyService.renommer(developerId, id, requete.name()))
                .map(CleApiResponse::depuis);
    }

    @Operation(summary = "Révoquer une clé API", description = "Révocation immédiate ; la clé ne peut plus s'authentifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clé révoquée"),
            @ApiResponse(responseCode = "404", description = "Clé introuvable")
    })
    @PostMapping("/{id}:revoke")
    public Mono<CleApiResponse> revoquer(@PathVariable UUID id) {
        return developpeurCourant.id()
                .flatMap(developerId -> apiKeyService.revoquer(developerId, id))
                .map(CleApiResponse::depuis);
    }
}
