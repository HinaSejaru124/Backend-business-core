package com.yowyob.businesscore.adapter.in.rest.rule;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.rule.GestionRegleService;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Contenu de version", description = "Offres, rôles, règles, opérations et configuration d'une version")
@RestController
@RequestMapping("/v1")
public class RegleMetierController {

    private final GestionRegleService gestion;

    public RegleMetierController(GestionRegleService gestion) {
        this.gestion = gestion;
    }

    @Operation(summary = "Créer une règle de type",
            description = "Déclare une règle métier sur une version de type (portée TYPE).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Règle créée"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
    @PostMapping("/business-types/{typeId}/versions/{versionN}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegleMetierResponse> creerRegleDeType(
            @PathVariable UUID typeId,
            @PathVariable int versionN,
            @Valid @RequestBody CreerRegleRequest body) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> gestion.creerRegleDeType(
                        typeId, versionN, body.declencheur(), body.condition(),
                        body.effet(), body.rolesAutorisesADeroger(), ctx))
                .map(RegleMetierResponse::de);
    }

    @Operation(summary = "Lister les règles de type")
    @ApiResponse(responseCode = "200", description = "Liste des règles de la version")
    @GetMapping("/business-types/{typeId}/versions/{versionN}/rules")
    public Flux<RegleMetierResponse> listerReglesDeType(
            @PathVariable UUID typeId, @PathVariable int versionN) {
        return gestion.listerParVersion(typeId, versionN).map(RegleMetierResponse::de);
    }

    @Operation(summary = "Consulter une règle de type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La règle"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @GetMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    public Mono<RegleMetierResponse> trouverRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId) {
        return gestion.trouverDeType(typeId, versionN, ruleId).map(RegleMetierResponse::de);
    }

    @Operation(summary = "Modifier une règle de type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Règle mise à jour"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @PutMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    public Mono<RegleMetierResponse> modifierRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId,
            @Valid @RequestBody CreerRegleRequest body) {
        return gestion.modifierDeType(typeId, versionN, ruleId, body.declencheur(),
                        body.condition(), body.effet(), body.rolesAutorisesADeroger())
                .map(RegleMetierResponse::de);
    }

    @Operation(summary = "Supprimer une règle de type")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Règle supprimée"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @DeleteMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimerRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId) {
        return gestion.supprimerDeType(typeId, versionN, ruleId);
    }

    @Operation(summary = "Créer une règle locale",
            description = "Déclare une règle spécifique à une entreprise (portée ENTREPRISE).",
            tags = {"Entreprises"})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Règle locale créée"),
            @ApiResponse(responseCode = "404", description = "Entreprise introuvable")
    })
    @PostMapping("/businesses/{businessId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegleMetierResponse> creerRegleLocale(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreerRegleRequest body) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> gestion.creerRegleLocale(
                        businessId, body.declencheur(), body.condition(),
                        body.effet(), body.rolesAutorisesADeroger(), ctx))
                .map(RegleMetierResponse::de);
    }

    @Operation(summary = "Lister les règles locales", tags = {"Entreprises"})
    @ApiResponse(responseCode = "200", description = "Liste des règles de l'entreprise")
    @GetMapping("/businesses/{businessId}/rules")
    public Flux<RegleMetierResponse> listerReglesLocales(@PathVariable UUID businessId) {
        return gestion.listerParEntreprise(businessId).map(RegleMetierResponse::de);
    }

    @Operation(summary = "Consulter une règle locale", tags = {"Entreprises"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La règle locale"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @GetMapping("/businesses/{businessId}/rules/{ruleId}")
    public Mono<RegleMetierResponse> trouverRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId) {
        return gestion.trouverLocale(businessId, ruleId).map(RegleMetierResponse::de);
    }

    @Operation(summary = "Modifier une règle locale", tags = {"Entreprises"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Règle locale mise à jour"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @PutMapping("/businesses/{businessId}/rules/{ruleId}")
    public Mono<RegleMetierResponse> modifierRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId,
            @Valid @RequestBody CreerRegleRequest body) {
        return gestion.modifierLocale(businessId, ruleId, body.declencheur(),
                        body.condition(), body.effet(), body.rolesAutorisesADeroger())
                .map(RegleMetierResponse::de);
    }

    @Operation(summary = "Supprimer une règle locale", tags = {"Entreprises"})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Règle locale supprimée"),
            @ApiResponse(responseCode = "404", description = "Règle introuvable")
    })
    @DeleteMapping("/businesses/{businessId}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimerRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId) {
        return gestion.supprimerLocale(businessId, ruleId);
    }

    @Schema(description = "Corps de création ou modification d'une règle métier")
    public record CreerRegleRequest(
            @Schema(description = "Moment d'évaluation de la règle", example = "AVANT_OPERATION")
            @NotNull Declencheur declencheur,
            @Schema(description = "Expression de condition (SpEL ou DSL métier)", example = "montant > 1000")
            @NotBlank String condition,
            @Schema(description = "Effet si la condition est vraie", example = "BLOQUER")
            @NotNull Effet effet,
            @Schema(description = "Codes de rôles autorisés à déroger", example = "[\"MANAGER\"]")
            List<String> rolesAutorisesADeroger
    ) {}

    @Schema(description = "Règle métier (portée TYPE ou ENTREPRISE)")
    public record RegleMetierResponse(
            @Schema(example = "00000000-0000-0000-0000-000000000000") UUID id,
            @Schema(example = "AVANT_OPERATION") String declencheur,
            @Schema(example = "montant > 1000") String condition,
            @Schema(example = "BLOQUER") String effet,
            List<String> rolesAutorisesADeroger,
            @Schema(description = "TYPE ou ENTREPRISE", example = "TYPE") String portee
    ) {
        public static RegleMetierResponse de(RegleMetier r) {
            return new RegleMetierResponse(
                    r.getId(),
                    r.getDeclencheur().name(),
                    r.getCondition(),
                    r.getEffet().name(),
                    r.getRolesAutorisesADeroger(),
                    r.estDeType() ? "TYPE" : "ENTREPRISE");
        }
    }
}
