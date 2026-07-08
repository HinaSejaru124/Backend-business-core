package com.yowyob.businesscore.adapter.in.rest.rule;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.rule.GestionRegleService;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
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

/**
 * API REST de la brique Règles — CRUD Type et locales.
 */
@RestController
@RequestMapping("/v1")
public class RegleMetierController {

    private final GestionRegleService gestion;

    public RegleMetierController(GestionRegleService gestion) {
        this.gestion = gestion;
    }

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

    @GetMapping("/business-types/{typeId}/versions/{versionN}/rules")
    public Flux<RegleMetierResponse> listerReglesDeType(
            @PathVariable UUID typeId, @PathVariable int versionN) {
        return gestion.listerParVersion(typeId, versionN).map(RegleMetierResponse::de);
    }

    @GetMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    public Mono<RegleMetierResponse> trouverRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId) {
        return gestion.trouverDeType(typeId, versionN, ruleId).map(RegleMetierResponse::de);
    }

    @PutMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    public Mono<RegleMetierResponse> modifierRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId,
            @Valid @RequestBody CreerRegleRequest body) {
        return gestion.modifierDeType(typeId, versionN, ruleId, body.declencheur(),
                        body.condition(), body.effet(), body.rolesAutorisesADeroger())
                .map(RegleMetierResponse::de);
    }

    @DeleteMapping("/business-types/{typeId}/versions/{versionN}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimerRegleDeType(
            @PathVariable UUID typeId, @PathVariable int versionN, @PathVariable UUID ruleId) {
        return gestion.supprimerDeType(typeId, versionN, ruleId);
    }

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

    @GetMapping("/businesses/{businessId}/rules")
    public Flux<RegleMetierResponse> listerReglesLocales(@PathVariable UUID businessId) {
        return gestion.listerParEntreprise(businessId).map(RegleMetierResponse::de);
    }

    @GetMapping("/businesses/{businessId}/rules/{ruleId}")
    public Mono<RegleMetierResponse> trouverRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId) {
        return gestion.trouverLocale(businessId, ruleId).map(RegleMetierResponse::de);
    }

    @PutMapping("/businesses/{businessId}/rules/{ruleId}")
    public Mono<RegleMetierResponse> modifierRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId,
            @Valid @RequestBody CreerRegleRequest body) {
        return gestion.modifierLocale(businessId, ruleId, body.declencheur(),
                        body.condition(), body.effet(), body.rolesAutorisesADeroger())
                .map(RegleMetierResponse::de);
    }

    @DeleteMapping("/businesses/{businessId}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimerRegleLocale(
            @PathVariable UUID businessId, @PathVariable UUID ruleId) {
        return gestion.supprimerLocale(businessId, ruleId);
    }

    public record CreerRegleRequest(
            @NotNull Declencheur declencheur,
            @NotBlank String condition,
            @NotNull Effet effet,
            List<String> rolesAutorisesADeroger
    ) {}

    public record RegleMetierResponse(
            UUID id,
            String declencheur,
            String condition,
            String effet,
            List<String> rolesAutorisesADeroger,
            String portee
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
