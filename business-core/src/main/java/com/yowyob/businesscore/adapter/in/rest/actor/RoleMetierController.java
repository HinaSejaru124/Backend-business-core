package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.DeclarerRoleCommande;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.ModifierRoleCommande;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
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
 * API REST — déclaration des rôles métier d'une version de Type. La version cible est résolue depuis
 * l'URL ({@code typeId} + {@code n}). Brique 3 (Acteurs métier).
 */
@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/roles")
public class RoleMetierController {

    private final GestionActeurService gestion;
    private final VersionTypeService versionService;

    public RoleMetierController(GestionActeurService gestion, VersionTypeService versionService) {
        this.gestion = gestion;
        this.versionService = versionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoleReponse> declarer(@PathVariable UUID typeId,
                                      @PathVariable int n,
                                      @Valid @RequestBody DeclarerRoleRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.declarerRole(
                        new DeclarerRoleCommande(version.id(), req.code(), req.categorie())))
                .map(RoleReponse::de);
    }

    @GetMapping
    public Flux<RoleReponse> lister(@PathVariable UUID typeId, @PathVariable int n) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMapMany(version -> gestion.listerRoles(version.id()))
                .map(RoleReponse::de);
    }

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
