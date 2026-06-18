package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur;
import com.yowyob.businesscore.domain.port.in.actor.GestionActeur.RattacherActeurCommande;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/businesses/{businessId}/actors")
public class ActeurMetierController {

    private final GestionActeur gestion;

    public ActeurMetierController(GestionActeur gestion) {
        this.gestion = gestion;
    }

    public record RattacherActeurRequete(
            @NotNull UUID roleMetierId,
            @NotBlank String identifiantPersonne) {}

    public record ActeurReponse(
            UUID id, UUID entrepriseId, UUID roleMetierId, UUID acteurKernelId,
            Instant valideDepuis, Instant valideJusqua) {
        static ActeurReponse de(ActeurMetier a) {
            return new ActeurReponse(a.id(), a.entrepriseId(), a.roleMetierId(),
                    a.acteurKernelId(), a.valideDepuis(), a.valideJusqua());
        }
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

    @DeleteMapping("/{actorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> detacher(@PathVariable UUID businessId, @PathVariable UUID actorId) {
        return gestion.detacher(businessId, actorId);
    }
}