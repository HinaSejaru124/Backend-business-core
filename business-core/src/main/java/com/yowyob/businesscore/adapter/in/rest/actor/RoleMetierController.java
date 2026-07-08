package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.DeclarerRoleCommande;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.ModifierRoleCommande;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/roles")
public class RoleMetierController {

    private final GestionActeurService gestion;
    private final VersionTypeService versionService;

    public RoleMetierController(GestionActeurService gestion, VersionTypeService versionService) {
        this.gestion = gestion;
        this.versionService = versionService;
    }

    @Operation(summary = "Déclarer un rôle métier",
            description = "Ajoute un rôle (code + catégorie) à la version de type.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rôle déclaré"),
            @ApiResponse(responseCode = "404", description = "Version introuvable"),
            @ApiResponse(responseCode = "409", description = "Code de rôle déjà utilisé")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoleReponse> declarer(@PathVariable UUID typeId,
                                      @Parameter(example = "1") @PathVariable int n,
                                      @Valid @RequestBody DeclarerRoleRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.declarerRole(
                        new DeclarerRoleCommande(version.id(), req.code(), req.categorie())))
                .map(RoleReponse::de);
    }

    @Operation(summary = "Lister les rôles métier")
    @ApiResponse(responseCode = "200", description = "Liste des rôles")
    @GetMapping
    public Flux<RoleReponse> lister(@PathVariable UUID typeId, @PathVariable int n) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMapMany(version -> gestion.listerRoles(version.id()))
                .map(RoleReponse::de);
    }

    @Operation(summary = "Modifier un rôle métier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôle mis à jour"),
            @ApiResponse(responseCode = "404", description = "Rôle introuvable")
    })
    @PutMapping("/{roleId}")
    public Mono<RoleReponse> modifier(@PathVariable UUID typeId,
                                      @PathVariable int n,
                                      @PathVariable UUID roleId,
                                      @Valid @RequestBody ModifierRoleRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.modifierRole(
                        new ModifierRoleCommande(version.id(), roleId, req.code())))
                .map(RoleReponse::de);
    }

    @Operation(summary = "Supprimer un rôle métier")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rôle supprimé"),
            @ApiResponse(responseCode = "404", description = "Rôle introuvable"),
            @ApiResponse(responseCode = "409", description = "Rôle encore utilisé par des acteurs")
    })
    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimer(@PathVariable UUID typeId,
                                @PathVariable int n,
                                @PathVariable UUID roleId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.supprimerRole(version.id(), roleId));
    }
}
