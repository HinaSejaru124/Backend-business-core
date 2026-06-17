package com.yowyob.businesscore.adapter.in.rest.businesstype;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.businesstype.TypeMetierService;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
import com.yowyob.businesscore.application.usecase.configuration.ConfigurationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controller REST — Brique 1 (Type Métier) + Brique 7 (Configuration).
 *
 * Toutes les routes sont protégées par la clé Business Core (SecurityConfig du socle).
 * Le BusinessContext est extrait du contexte réactif (posé par BusinessContextWebFilter).
 *
 * Routes exposées :
 *   POST   /v1/business-types                               → créer un type
 *   GET    /v1/business-types                               → lister les types du tenant
 *   GET    /v1/business-types/{typeId}                      → voir un type
 *   POST   /v1/business-types/{typeId}/publish              → publier un type
 *   POST   /v1/business-types/{typeId}/archive              → archiver un type
 *   POST   /v1/business-types/{typeId}/versions             → créer une version
 *   GET    /v1/business-types/{typeId}/versions             → lister les versions
 *   GET    /v1/business-types/{typeId}/versions/{versionNumber} → voir une version
 *   POST   /v1/business-types/{typeId}/versions/{versionNumber}/publish → publier une version
 *   POST   /v1/business-types/{typeId}/versions/{versionNumber}/config  → définir un paramètre
 *   GET    /v1/business-types/{typeId}/versions/{versionNumber}/config  → lister les paramètres
 */
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

    // ══════════════════════════════════════════════════════════════════════
    // Types Métier
    // ══════════════════════════════════════════════════════════════════════

    /** POST /v1/business-types — Créer un Type Métier en BROUILLON */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TypeMetierResponse> creer(
            @Valid @RequestBody CreerTypeRequest req) {

        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.creer(
                        req.code(), req.nom(),
                        req.domainCode(), req.domainNom(),
                        ctx))
                .map(TypeMetierResponse::depuis);
    }

    /** GET /v1/business-types — Lister les Types Métier du tenant */
    @GetMapping
    public Flux<TypeMetierResponse> lister() {
        return BusinessContextHolder.currentContext()
                .flatMapMany(typeService::lister)
                .map(TypeMetierResponse::depuis);
    }

    /** GET /v1/business-types/{typeId} — Voir un Type Métier */
    @GetMapping("/{typeId}")
    public Mono<TypeMetierResponse> trouver(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.trouverParId(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    /** POST /v1/business-types/{typeId}/publish — Publier : BROUILLON → PUBLIE */
    @PostMapping("/{typeId}/publish")
    public Mono<TypeMetierResponse> publier(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.publier(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    /** POST /v1/business-types/{typeId}/archive — Archiver : PUBLIE → ARCHIVE */
    @PostMapping("/{typeId}/archive")
    public Mono<TypeMetierResponse> archiver(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> typeService.archiver(typeId, ctx))
                .map(TypeMetierResponse::depuis);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Versions
    // ══════════════════════════════════════════════════════════════════════

    /** POST /v1/business-types/{typeId}/versions — Créer une nouvelle version */
    @PostMapping("/{typeId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<VersionTypeResponse> creerVersion(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.creerVersion(typeId, ctx))
                .map(VersionTypeResponse::depuis);
    }

    /** GET /v1/business-types/{typeId}/versions — Lister les versions */
    @GetMapping("/{typeId}/versions")
    public Flux<VersionTypeResponse> listerVersions(@PathVariable UUID typeId) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> versionService.lister(typeId, ctx))
                .map(VersionTypeResponse::depuis);
    }

    /** GET /v1/business-types/{typeId}/versions/{versionNumber} — Voir une version */
    @GetMapping("/{typeId}/versions/{versionNumber}")
    public Mono<VersionTypeResponse> trouverVersion(@PathVariable UUID typeId,
                                                    @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx))
                .map(VersionTypeResponse::depuis);
    }

    /** POST /v1/business-types/{typeId}/versions/{versionNumber}/publish — Publier une version */
    @PostMapping("/{typeId}/versions/{versionNumber}/publish")
    public Mono<VersionTypeResponse> publierVersion(@PathVariable UUID typeId,
                                                    @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.publierVersion(typeId, versionNumber, ctx))
                .map(VersionTypeResponse::depuis);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Configuration
    // ══════════════════════════════════════════════════════════════════════

    /** POST /v1/business-types/{typeId}/versions/{versionNumber}/config — Définir un paramètre */
    @PostMapping("/{typeId}/versions/{versionNumber}/config")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ParametreConfigResponse> definirParametre(@PathVariable UUID typeId,
                                                          @PathVariable int versionNumber,
                                                          @Valid @RequestBody DefinirParametreRequest req) {
        // Résolution numéro → version (revérifie aussi l'appartenance au tenant) avant d'écrire le paramètre.
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx)
                        .flatMap(v -> configService.definirPourType(
                                v.id(), req.cle(), req.valeur(), req.verrouille(), ctx)))
                .map(ParametreConfigResponse::depuis);
    }

    /** GET /v1/business-types/{typeId}/versions/{versionNumber}/config — Lister les paramètres */
    @GetMapping("/{typeId}/versions/{versionNumber}/config")
    public Flux<ParametreConfigResponse> listerParametres(@PathVariable UUID typeId,
                                                          @PathVariable int versionNumber) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> versionService.trouverParNumero(typeId, versionNumber, ctx)
                        .flatMapMany(v -> configService.listerParVersion(v.id())))
                .map(ParametreConfigResponse::depuis);
    }
}
