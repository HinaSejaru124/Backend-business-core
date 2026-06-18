package com.yowyob.businesscore.adapter.in.rest.offer;

import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.in.offer.GestionOffre;
import com.yowyob.businesscore.domain.port.in.offer.GestionOffre.DeclarerOffreCommande;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/business-types/{typeId}/versions/{n}/offers")
public class OffreController {

    private final GestionOffre gestion;

    public OffreController(GestionOffre gestion) {
        this.gestion = gestion;
    }

    public record DeclarerOffreRequete(
            @NotNull UUID versionTypeId,
            @NotBlank String nom,
            @NotNull FormePrix formePrix,
            BigDecimal prix,
            Set<TypeCapacite> capacites) {}

    public record CapaciteReponse(UUID id, TypeCapacite type, boolean active) {
        static CapaciteReponse de(Capacite c) {
            return new CapaciteReponse(c.id(), c.type(), c.active());
        }
    }

    public record OffreReponse(
            UUID id, UUID versionTypeId, String nom, FormePrix formePrix,
            BigDecimal prix, List<CapaciteReponse> capacites) {
        static OffreReponse de(DefinitionOffre o) {
            return new OffreReponse(o.id(), o.versionTypeId(), o.nom(), o.formePrix(), o.prix(),
                    o.capacites().stream().map(CapaciteReponse::de).toList());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OffreReponse> declarer(@PathVariable UUID typeId,
                                       @PathVariable int n,
                                       @Valid @RequestBody DeclarerOffreRequete req) {
        return gestion.declarer(new DeclarerOffreCommande(
                        req.versionTypeId(), req.nom(), req.formePrix(), req.prix(), req.capacites()))
                .map(OffreReponse::de);
    }

    @GetMapping
    public Flux<OffreReponse> lister(@PathVariable UUID typeId,
                                     @PathVariable int n,
                                     @RequestParam UUID versionTypeId) {
        return gestion.lister(versionTypeId).map(OffreReponse::de);
    }
}