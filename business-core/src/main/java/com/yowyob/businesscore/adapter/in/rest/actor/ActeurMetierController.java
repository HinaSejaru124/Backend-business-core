package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.ModifierActeurCommande;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.RattacherActeurCommande;
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

@Tag(name = "Entreprises", description = "Instances de métier (niveau Entreprise)")
@RestController
@RequestMapping("/v1/businesses/{businessId}/actors")
public class ActeurMetierController {

    private final GestionActeurService gestion;

    public ActeurMetierController(GestionActeurService gestion) {
        this.gestion = gestion;
    }

    @Operation(summary = "Rattacher un acteur",
            description = "Associe une personne (identifiant kernel) à un rôle métier de l'entreprise.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Acteur rattaché"),
            @ApiResponse(responseCode = "404", description = "Entreprise ou rôle introuvable"),
            @ApiResponse(responseCode = "409", description = "Acteur déjà rattaché")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ActeurReponse> rattacher(@PathVariable UUID businessId,
                                         @Valid @RequestBody RattacherActeurRequete req) {
        return gestion.rattacher(new RattacherActeurCommande(
                        businessId, req.roleMetierId(), req.identifiantPersonne()))
                .map(ActeurReponse::de);
    }

    @Operation(summary = "Lister les acteurs de l'entreprise")
    @ApiResponse(responseCode = "200", description = "Liste des acteurs")
    @GetMapping
    public Flux<ActeurReponse> lister(@PathVariable UUID businessId) {
        return gestion.lister(businessId).map(ActeurReponse::de);
    }

    @Operation(summary = "Consulter un acteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "L'acteur"),
            @ApiResponse(responseCode = "404", description = "Acteur introuvable")
    })
    @GetMapping("/{actorId}")
    public Mono<ActeurReponse> trouver(
            @PathVariable UUID businessId,
            @Parameter(description = "Identifiant de l'acteur métier") @PathVariable UUID actorId) {
        return gestion.trouver(businessId, actorId).map(ActeurReponse::de);
    }

    @Operation(summary = "Modifier le rôle d'un acteur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acteur mis à jour"),
            @ApiResponse(responseCode = "404", description = "Acteur ou rôle introuvable")
    })
    @PutMapping("/{actorId}")
    public Mono<ActeurReponse> modifier(@PathVariable UUID businessId,
                                        @PathVariable UUID actorId,
                                        @Valid @RequestBody ModifierActeurRequete req) {
        return gestion.modifier(new ModifierActeurCommande(businessId, actorId, req.roleMetierId()))
                .map(ActeurReponse::de);
    }

    @Operation(summary = "Détacher un acteur", description = "Supprime le rattachement acteur ↔ entreprise.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Acteur détaché"),
            @ApiResponse(responseCode = "404", description = "Acteur introuvable")
    })
    @DeleteMapping("/{actorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> detacher(@PathVariable UUID businessId, @PathVariable UUID actorId) {
        return gestion.detacher(businessId, actorId);
    }
}
