// adapter/in/rest/rule/RegleMetierController.java
package com.yowyob.businesscore.adapter.in.rest.rule;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.usecase.rule.CreerRegleUseCase;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * API REST de la brique Règles. Deux points d'entrée de création :
 * <ul>
 *   <li>règle de Type : {@code POST /v1/business-types/{typeId}/versions/{versionN}/rules} ;</li>
 *   <li>règle locale : {@code POST /v1/businesses/{businessId}/rules}.</li>
 * </ul>
 * Le {@code BusinessContext} (donc le tenant) est injecté par le socle ; le corps est validé
 * ({@code @Valid}) avant d'atteindre le use case.
 */
@RestController
@RequestMapping("/v1")
public class RegleMetierController {

    private final CreerRegleUseCase creerRegle;

    public RegleMetierController(CreerRegleUseCase creerRegle) {
        this.creerRegle = creerRegle;
    }

    /** POST /v1/business-types/{typeId}/versions/{n}/rules */
    @PostMapping("/business-types/{typeId}/versions/{versionN}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegleMetierResponse> creerRegleDeType(
            @PathVariable UUID typeId,
            @PathVariable int versionN,
            @Valid @RequestBody CreerRegleRequest body,
            BusinessContext ctx) {

        // L'URL est la source de vérité : la version cible est résolue depuis typeId + versionN.
        return creerRegle.creerRegleDeType(
                typeId,
                versionN,
                body.declencheur(),
                body.condition(),
                body.effet(),
                body.rolesAutorisesADeroger(),
                ctx
        ).map(RegleMetierResponse::de);
    }

    /** POST /v1/businesses/{businessId}/rules */
    @PostMapping("/businesses/{businessId}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegleMetierResponse> creerRegleLocale(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreerRegleRequest body,
            BusinessContext ctx) {

        return creerRegle.creerRegleLocale(
                businessId,
                body.declencheur(),
                body.condition(),
                body.effet(),
                body.rolesAutorisesADeroger(),
                ctx
        ).map(RegleMetierResponse::de);
    }

    // --- DTOs ---

    /**
     * Corps aligné sur le schéma OpenAPI {@code CreateRule} : pas d'identifiant de version
     * (la version cible provient de l'URL). {@code declencheur} et {@code effet} obligatoires.
     */
    public record CreerRegleRequest(
            @NotNull  Declencheur declencheur,
            @NotBlank String      condition,      // ex. "CATEGORIE_EGALE:valeur=medicament"
            @NotNull  Effet       effet,
            List<String>          rolesAutorisesADeroger
    ) {}

    public record RegleMetierResponse(
            UUID         id,
            String       declencheur,
            String       condition,
            String       effet,
            List<String> rolesAutorisesADeroger,
            String       portee          // TYPE | ENTREPRISE (cf. schéma OpenAPI Rule)
    ) {
        public static RegleMetierResponse de(RegleMetier r) {
            return new RegleMetierResponse(
                    r.getId(),
                    r.getDeclencheur().name(),
                    r.getCondition(),
                    r.getEffet().name(),
                    r.getRolesAutorisesADeroger(),
                    r.estDeType() ? "TYPE" : "ENTREPRISE"
            );
        }
    }
}