package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
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

@Tag(name = "Entreprises", description = "Instances de métier épinglées à une version de Type")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/businesses")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @Operation(summary = "Créer une entreprise",
            description = """
                    Instancie un Type Métier à une version donnée. Provisionne l'organisation kernel si absente
                    (actor → org → approve → services → agence principale).""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entreprise créée"),
            @ApiResponse(responseCode = "404", description = "Version de type introuvable"),
            @ApiResponse(responseCode = "422", description = "Données invalides")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EntrepriseResponse> creer(@Valid @RequestBody CreerEntrepriseRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.creer(
                        requete.typeId(), requete.versionNumber(), requete.nom(),
                        requete.organizationId(), ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Lister les entreprises", description = "Retourne toutes les entreprises du tenant courant.")
    @ApiResponse(responseCode = "200", description = "Liste des entreprises")
    @GetMapping
    public Flux<EntrepriseResponse> lister() {
        return BusinessContextHolder.currentContext()
                .flatMapMany(entrepriseService::lister)
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Consulter une entreprise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'entreprise"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @GetMapping("/{businessId}")
    public Mono<EntrepriseResponse> trouver(
            @Parameter(description = "Identifiant de l'entreprise") @PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Modifier une entreprise",
            description = "Met à jour les métadonnées locales (nom). Pas de rename de l'organisation kernel.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entreprise mise à jour"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @PutMapping("/{businessId}")
    public Mono<EntrepriseResponse> modifier(@PathVariable UUID businessId,
                                             @Valid @RequestBody ModifierEntrepriseRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.modifier(businessId, requete.nom(), ctx))
                .map(EntrepriseResponse::depuis);
    }

    @Operation(summary = "Archiver une entreprise",
            description = "Passe le cycle de vie en FERMEE (local + kernel `close`). Pas de suppression dure.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Entreprise archivée"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
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
            @ApiResponse(responseCode = "200", description = "Entreprise approuvée (cycleVie ACTIVE)"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable"),
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
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @PutMapping("/{businessId}/lifecycle")
    public Mono<EntrepriseResponse> changerCycleVie(@PathVariable UUID businessId,
                                                    @Valid @RequestBody ChangerCycleVieRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.changerCycleVie(businessId, requete.cycleVie(), ctx))
                .map(EntrepriseResponse::depuis);
    }
}
