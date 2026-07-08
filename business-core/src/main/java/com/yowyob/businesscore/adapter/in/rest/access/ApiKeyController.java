package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Accès", description = "Inscription et gestion des clés d'API")
@RestController
@RequestMapping("/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final DeveloperAccountRepository developerRepository;

    public ApiKeyController(ApiKeyService apiKeyService,
                           DeveloperAccountRepository developerRepository) {
        this.apiKeyService = apiKeyService;
        this.developerRepository = developerRepository;
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
        return developerCourant()
                .flatMap(developerId -> apiKeyService.creer(developerId, nom))
                .map(CleApiCreeeResponse::depuis);
    }

    @Operation(summary = "Lister ses clés API", description = "Retourne les clés sans le secret.")
    @ApiResponse(responseCode = "200", description = "Liste des clés")
    @GetMapping
    public Flux<CleApiResponse> lister() {
        return developerCourant()
                .flatMapMany(apiKeyService::lister)
                .map(CleApiResponse::depuis);
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
        return developerCourant()
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
        return developerCourant()
                .flatMap(developerId -> apiKeyService.revoquer(developerId, id))
                .map(CleApiResponse::depuis);
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
