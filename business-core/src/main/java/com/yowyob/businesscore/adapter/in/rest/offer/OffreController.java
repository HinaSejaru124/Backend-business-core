package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.businesstype.VersionTypeService;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService.DeclarerOffreCommande;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API REST — Brique 2 (Offre). La version cible est résolue depuis l'URL ({@code typeId} + {@code n}),
 * jamais reprise du payload. Le {@code BusinessContext} est posé par le filtre du socle.
 */
@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/offers")
public class OffreController {

    private final GestionOffreService gestion;
    private final VersionTypeService versionService;

    public OffreController(GestionOffreService gestion, VersionTypeService versionService) {
        this.gestion = gestion;
        this.versionService = versionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OffreReponse> declarer(@PathVariable UUID typeId,
                                       @PathVariable int n,
                                       @Valid @RequestBody DeclarerOffreRequete req) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMap(version -> gestion.declarer(new DeclarerOffreCommande(
                        version.id(), req.nom(), req.formePrix(), req.prix(), req.capacites())))
                .map(OffreReponse::de);
    }

    @GetMapping
    public Flux<OffreReponse> lister(@PathVariable UUID typeId, @PathVariable int n) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> versionService.trouverParNumero(typeId, n, ctx))
                .flatMapMany(version -> gestion.lister(version.id()))
                .map(OffreReponse::de);
    }
}
