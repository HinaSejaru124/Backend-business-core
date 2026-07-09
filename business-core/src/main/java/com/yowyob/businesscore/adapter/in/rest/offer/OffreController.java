package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService.DeclarerOffreCommande;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService.ModifierOffreCommande;
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

@Tag(name = "Contenu de version", description = "Offres, rôles, règles, opérations et configuration d'une version")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/offers")
public class OffreController {

    private final GestionOffreService gestion;
    private final VersionTypeService versionService;

    public OffreController(GestionOffreService gestion, VersionTypeService versionService) {
        this.gestion = gestion;
        this.versionService = versionService;
    }

    @Operation(summary = "Déclarer une offre",
            description = "Définit une offre (nom, forme de prix, capacités) sur la version.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Offre déclarée"),
            @ApiResponse(responseCode = "404", description = "Version introuvable")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OffreReponse> declarer(@PathVariable UUID typeId,
                                       @Parameter(example = "1") @PathVariable int n,
                                       @Valid @RequestBody DeclarerOffreRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.declarer(new DeclarerOffreCommande(
                        version.id(), req.nom(), req.formePrix(), req.prix(), req.capacites())))
                .map(OffreReponse::de);
    }

    @Operation(summary = "Lister les offres")
    @ApiResponse(responseCode = "200", description = "Liste des offres")
    @GetMapping
    public Flux<OffreReponse> lister(@PathVariable UUID typeId, @PathVariable int n) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMapMany(version -> gestion.lister(version.id()))
                .map(OffreReponse::de);
    }

    @Operation(summary = "Consulter une offre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'offre"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @GetMapping("/{offerId}")
    public Mono<OffreReponse> trouver(@PathVariable UUID typeId,
                                      @PathVariable int n,
                                      @PathVariable UUID offerId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.trouver(version.id(), offerId))
                .map(OffreReponse::de);
    }

    @Operation(summary = "Modifier une offre")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Offre mise à jour"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @PutMapping("/{offerId}")
    public Mono<OffreReponse> modifier(@PathVariable UUID typeId,
                                       @PathVariable int n,
                                       @PathVariable UUID offerId,
                                       @Valid @RequestBody DeclarerOffreRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.modifier(new ModifierOffreCommande(
                        version.id(), offerId, req.nom(), req.formePrix(), req.prix(), req.capacites())))
                .map(OffreReponse::de);
    }

    @Operation(summary = "Supprimer une offre")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Offre supprimée"),
            @ApiResponse(responseCode = "404", description = "Offre introuvable")
    })
    @DeleteMapping("/{offerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimer(@PathVariable UUID typeId,
                                @PathVariable int n,
                                @PathVariable UUID offerId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.supprimer(version.id(), offerId));
    }
}
