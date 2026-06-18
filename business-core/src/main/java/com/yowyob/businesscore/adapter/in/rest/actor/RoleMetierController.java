package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur.DeclarerRoleCommande;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/roles")
public class RoleMetierController {

    private final GestionActeur gestion;

    public RoleMetierController(GestionActeur gestion) {
        this.gestion = gestion;
    }

    public record DeclarerRoleRequete(
            @NotNull UUID versionTypeId,
            @NotBlank String code,
            @NotNull CategorieActeur categorie) {}

    public record RoleReponse(UUID id, UUID versionTypeId, String code, CategorieActeur categorie) {
        static RoleReponse de(RoleMetier r) {
            return new RoleReponse(r.id(), r.versionTypeId(), r.code(), r.categorie());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoleReponse> declarer(@PathVariable UUID typeId,
                                      @PathVariable int n,
                                      @Valid @RequestBody DeclarerRoleRequete req) {
        return gestion.declarerRole(new DeclarerRoleCommande(req.versionTypeId(), req.code(), req.categorie()))
                .map(RoleReponse::de);
    }
}