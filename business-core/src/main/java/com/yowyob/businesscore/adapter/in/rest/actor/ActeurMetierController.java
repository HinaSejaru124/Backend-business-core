package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.application.usecase.actor.GestionActeurService;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.ModifierActeurCommande;
import com.yowyob.businesscore.application.usecase.actor.GestionActeurService.RattacherActeurCommande;
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
 * API REST — rattachement des acteurs métier à une entreprise. Brique 3 (Acteurs métier).
 * Le tenant (BusinessContext) est propagé par le filtre du socle dans le contexte réactif.
 */
@RestController
@RequestMapping("/v1/businesses/{businessId}/actors")
public class ActeurMetierController {

    private final GestionActeurService gestion;

    public ActeurMetierController(GestionActeurService gestion) {
        this.gestion = gestion;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ActeurReponse> rattacher(@PathVariable UUID businessId,
                                         @Valid @RequestBody RattacherActeurRequete req) {
        return gestion.rattacher(new RattacherActeurCommande(
                        businessId, req.roleMetierId(), req.identifiantPersonne()))
                .map(ActeurReponse::de);
    }

    @GetMapping
    public Flux<ActeurReponse> lister(@PathVariable UUID businessId) {
        return gestion.lister(businessId).map(ActeurReponse::de);
    }

    @GetMapping("/{actorId}")
    public Mono<ActeurReponse> trouver(@PathVariable UUID businessId, @PathVariable UUID actorId) {
        return gestion.trouver(businessId, actorId).map(ActeurReponse::de);
    }

    @PutMapping("/{actorId}")
    public Mono<ActeurReponse> modifier(@PathVariable UUID businessId,
                                        @PathVariable UUID actorId,
                                        @Valid @RequestBody ModifierActeurRequete req) {
        return gestion.modifier(new ModifierActeurCommande(businessId, actorId, req.roleMetierId()))
                .map(ActeurReponse::de);
    }

    @DeleteMapping("/{actorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> detacher(@PathVariable UUID businessId, @PathVariable UUID actorId) {
        return gestion.detacher(businessId, actorId);
    }
}
