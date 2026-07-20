package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gestion des applications — JWT uniquement (cf. {@code SecurityConfig}), à l'exception de
 * {@code GET /v1/applications/me} qui est le point d'entrée réservé aux backends terminaux (clé Business
 * Core) : ces routes de gestion appellent le kernel au nom d'un développeur et sont destinées au front
 * Business Core, jamais consommées directement par un backend métier tiers.
 */
@Tag(name = "Applications", description = "Instances de métier épinglées à une version de Type")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/applications")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @Operation(summary = "Créer une application",
            description = """
                    Instancie un Type Métier à une version donnée. Si `organizationId` est fourni, l'application
                    se rattache à cette organisation kernel existante (aucune organisation créée). Sinon,
                    provisionne automatiquement une nouvelle organisation kernel (actor → org → approve →
                    services → agence principale).""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application créée"),
            @ApiResponse(responseCode = "404", description = "Version de type introuvable"),
            @ApiResponse(responseCode = "422", description = "Données invalides")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EntrepriseResponse> creer(@Valid @RequestBody CreerEntrepriseRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.creer(
                        requete.typeId(), requete.versionNumber(), requete.nom(), requete.organizationId(), ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Lister les applications", description = "Retourne toutes les applications du tenant courant.")
    @ApiResponse(responseCode = "200", description = "Liste des applications")
    @GetMapping
    public Flux<EntrepriseResponse> lister() {
        return BusinessContextHolder.currentContext()
                .flatMapMany(entrepriseService::lister)
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Résoudre l'application de la clé courante",
            description = """
                    Réservé aux backends terminaux (clé Business Core scopée) : renvoie l'application
                    représentée par la clé, sans que le terminal ait besoin de connaître ni transmettre
                    son businessId. Premier appel typique après réception des identifiants.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'application de la clé"),
            @ApiResponse(responseCode = "403", description = "Clé non scopée à une application")
    })
    @GetMapping("/me")
    public Mono<EntrepriseResponse> moi() {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> {
                    if (ctx.businessId() == null) {
                        return Mono.error(ProblemException.forbidden(
                                "Cette route nécessite une clé API scopée à une application."));
                    }
                    return entrepriseService.trouver(ctx.businessId(), ctx);
                })
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Consulter une application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'application"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @GetMapping("/{businessId}")
    public Mono<EntrepriseResponse> trouver(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Modifier une application",
            description = "Met à jour les métadonnées locales (nom). Pas de rename de l'organisation kernel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application mise à jour"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @PutMapping("/{businessId}")
    public Mono<EntrepriseResponse> modifier(@PathVariable UUID businessId,
                                             @Valid @RequestBody ModifierEntrepriseRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.modifier(businessId, requete.nom(), ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Archiver une application",
            description = "Passe le cycle de vie en FERMEE (local + kernel `close`). Pas de suppression dure.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Application archivée"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @DeleteMapping("/{businessId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> archiver(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.archiver(businessId, ctx));
    }

    @Operation(summary = "Approuver l'organisation kernel",
            description = """
                    Première approbation de gouvernance (`POST /api/organizations/{id}/approve`).
                    Distinct de `PUT .../lifecycle` avec ACTIVE (qui appelle `reopen`).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application approuvée (cycleVie ACTIVE)"),
            @ApiResponse(responseCode = "404", description = "Application introuvable"),
            @ApiResponse(responseCode = "422", description = "Pas d'organisation kernel à approuver")
    })
    @PostMapping("/{businessId}/approve")
    public Mono<EntrepriseResponse> approuver(@PathVariable UUID businessId,
                                              @RequestBody(required = false) ApprouverEntrepriseRequest requete) {
        String reason = requete == null ? null : requete.reason();
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.approuver(businessId, reason, ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Changer le cycle de vie",
            description = "Suspend, ferme ou rouvre l'organisation kernel (`suspend` / `close` / `reopen`).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cycle de vie mis à jour"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @PutMapping("/{businessId}/lifecycle")
    public Mono<EntrepriseResponse> changerCycleVie(@PathVariable UUID businessId,
                                                    @Valid @RequestBody ChangerCycleVieRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.changerCycleVie(businessId, requete.cycleVie(), ctx))
                .map(EntrepriseResponse::depuis);
    }
}
