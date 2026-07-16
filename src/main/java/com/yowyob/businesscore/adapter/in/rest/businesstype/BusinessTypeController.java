package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.businesstype.TypeMetierService;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
import com.yowyob.businesscore.application.usecase.configuration.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Types métier", description = "Déclaration du modèle métier (niveau Type)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/business-types")
public class BusinessTypeController {

    private final TypeMetierService   typeService;
    private final VersionTypeService  versionService;
    private final ConfigurationService configService;

    public BusinessTypeController(TypeMetierService typeService,
                                  VersionTypeService versionService,
                                  ConfigurationService configService) {
        this.typeService    = typeService;
        this.versionService = versionService;
        this.configService  = configService;
    }

    @Operation(summary = "Créer un type métier",
            description = "Crée un Type Métier en état BROUILLON pour le tenant courant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Type créé"),
            @ApiResponse(responseCode = "422", description = "Données invalides")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TypeMetierResponse> creer(@Valid @RequestBody CreerTypeRequest req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.creer(
                        req.code(), req.nom(),
                        req.domainCode(), req.domainNom(),
                        ctx))
                .map(TypeMetierResponse::depuis);
    }

    @Operation(summary = "Lister les types métier")
    @ApiResponse(responseCode = "200", description = "Liste des types du tenant")
    @GetMapping
    public Flux<TypeMetierResponse> lister() {
        return BusinessContextHolder.currentContext()
                .flatMapMany(typeService::lister)
                .map(TypeMetierResponse::depuis);
    }

    @Operation(summary = "Consulter un type métier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le type métier"),
            @ApiResponse(responseCode = "404", description = "Type introuvable")
    })
    @GetMapping("/{typeId}")
    public Mono<TypeMetierResponse> trouver(
            @Parameter(description = "Identifiant du type métier") @PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.trouverParId(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    @Operation(summary = "Publier un type métier", description = "Transition BROUILLON → PUBLIE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Type publié"),
            @ApiResponse(responseCode = "404", description = "Type introuvable"),
            @ApiResponse(responseCode = "409", description = "Transition invalide")
    })
    @PostMapping("/{typeId}/publish")
    public Mono<TypeMetierResponse> publier(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.publier(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    @Operation(summary = "Archiver un type métier", description = "Transition PUBLIE → ARCHIVE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Type archivé"),
            @ApiResponse(responseCode = "404", description = "Type introuvable")
    })
    @PostMapping("/{typeId}/archive")
    public Mono<TypeMetierResponse> archiver(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.archiver(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    @Operation(summary = "Créer une version", description = "Incrémente le numéro de version du type.",
            tags = {"Contenu de version"})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Version créée"),
            @ApiResponse(responseCode = "404", description = "Type introuvable")
    })
    @PostMapping("/{typeId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<VersionTypeResponse> creerVersion(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.creerVersion(typeId, ctx))
                .map(VersionTypeResponse::depuis);
    }

    @Operation(summary = "Lister les versions", tags = {"Contenu de version"})
    @ApiResponse(responseCode = "200", description = "Liste des versions")
    @GetMapping("/{typeId}/versions")
    public Flux<VersionTypeResponse> listerVersions(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> versionService.lister(typeId, ctx))
                .map(VersionTypeResponse::depuis);
    }

    @Operation(summary = "Consulter une version", tags = {"Contenu de version"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La version"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
    @GetMapping("/{typeId}/versions/{versionNumber}")
    public Mono<VersionTypeResponse> trouverVersion(@PathVariable UUID typeId,
                                                    @Parameter(example = "1") @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx))
                .map(VersionTypeResponse::depuis);
    }

    @Operation(summary = "Publier une version", description = "Rend la version utilisable pour instancier des entreprises.",
            tags = {"Contenu de version"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Version publiée"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
    @PostMapping("/{typeId}/versions/{versionNumber}/publish")
    public Mono<VersionTypeResponse> publierVersion(@PathVariable UUID typeId,
                                                    @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.publierVersion(typeId, versionNumber, ctx))
                .map(VersionTypeResponse::depuis);
    }

    @Operation(summary = "Définir un paramètre de configuration", tags = {"Contenu de version"})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paramètre enregistré"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
    @PostMapping("/{typeId}/versions/{versionNumber}/config")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ParametreConfigResponse> definirParametre(@PathVariable UUID typeId,
                                                          @PathVariable int versionNumber,
                                                          @Valid @RequestBody DefinirParametreRequest req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx)
                        .flatMap(v -> configService.definirPourType(
                                v.id(), req.cle(), req.valeur(), req.verrouille(), ctx)))
                .map(ParametreConfigResponse::depuis);
    }

    @Operation(summary = "Lister les paramètres de configuration", tags = {"Contenu de version"})
    @ApiResponse(responseCode = "200", description = "Liste des paramètres")
    @GetMapping("/{typeId}/versions/{versionNumber}/config")
    public Flux<ParametreConfigResponse> listerParametres(@PathVariable UUID typeId,
                                                          @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx)
                        .flatMapMany(v -> configService.listerParVersion(v.id())))
                .map(ParametreConfigResponse::depuis);
    }
}
