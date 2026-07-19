package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseProfilService;
import com.yowyob.businesscore.domain.enterprise.EnvironnementApplication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Fiche produit d'une application (entreprise) : description, logo, couleur, support, site web,
 * environnement. JWT uniquement (gestion de plateforme, cf. {@code SecurityConfig}).
 */
@Tag(name = "Fiche produit application", description = "Informations générales déclarées par le développeur (branding, identité)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping({"/v1/businesses/{businessId}/profile", "/v1/applications/{businessId}/profile"})
public class EntrepriseProfilController {

    private final EntrepriseProfilService profilService;

    public EntrepriseProfilController(EntrepriseProfilService profilService) {
        this.profilService = profilService;
    }

    @Operation(summary = "Consulter la fiche produit de l'application",
            description = "Crée une fiche vierge à la première consultation si l'application n'en a pas encore.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La fiche produit"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @GetMapping
    public Mono<EntrepriseProfilResponse> trouver(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> profilService.trouver(businessId, ctx))
                .map(EntrepriseProfilResponse::depuis);
    }

    @Operation(summary = "Modifier la fiche produit",
            description = "Remplace les informations générales. Un champ absent du corps efface la valeur précédente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche produit mise à jour"),
            @ApiResponse(responseCode = "400", description = "Champ invalide (URL, couleur ou e-mail)"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @PutMapping
    public Mono<EntrepriseProfilResponse> modifier(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId,
            @RequestBody ModifierProfilRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> profilService.modifier(businessId, requete.description(), requete.logoUrl(),
                        requete.couleur(), requete.supportEmail(), requete.siteWebUrl(),
                        parserEnvironnement(requete.environnement()), ctx))
                .map(EntrepriseProfilResponse::depuis);
    }

    private static EnvironnementApplication parserEnvironnement(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return EnvironnementApplication.valueOf(valeur.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ProblemException.badRequest("Environnement invalide : " + valeur);
        }
    }
}
