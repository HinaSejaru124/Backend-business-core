package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseContratService;
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
 * Contrat technique d'une application (entreprise) : clé publique + URLs de callback/succès/erreur/
 * annulation déclarées par le développeur. JWT uniquement (gestion de plateforme, cf. {@code SecurityConfig}).
 */
@Tag(name = "Contrat application", description = "Paramètres de communication déclarés entre l'application et Business Core")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/applications/{businessId}/contract")
public class EntrepriseContratController {

    private final EntrepriseContratService contratService;

    public EntrepriseContratController(EntrepriseContratService contratService) {
        this.contratService = contratService;
    }

    @Operation(summary = "Consulter le contrat technique de l'application",
            description = "Crée un contrat vierge à la première consultation si l'application n'en a pas encore.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le contrat"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @GetMapping
    public Mono<EntrepriseContratResponse> trouver(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> contratService.trouver(businessId, ctx))
                .map(EntrepriseContratResponse::depuis);
    }

    @Operation(summary = "Modifier les paramètres de communication",
            description = "Remplace les URLs de callback/succès/erreur/annulation. Une URL absente du corps efface la précédente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrat mis à jour"),
            @ApiResponse(responseCode = "400", description = "URL invalide"),
            @ApiResponse(responseCode = "404", description = "Application introuvable")
    })
    @PutMapping
    public Mono<EntrepriseContratResponse> modifier(
            @Parameter(description = "Identifiant de l'application") @PathVariable UUID businessId,
            @RequestBody ModifierContratRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> contratService.modifier(businessId, requete.callbackUrl(),
                        requete.successUrl(), requete.errorUrl(), requete.cancelUrl(), ctx))
                .map(EntrepriseContratResponse::depuis);
    }
}
