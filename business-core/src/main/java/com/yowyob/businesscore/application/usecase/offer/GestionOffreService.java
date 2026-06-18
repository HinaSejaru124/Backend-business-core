package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.application.capacite.RegistreCapacites;
import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.in.offer.GestionOffre;
import com.yowyob.businesscore.domain.port.out.offer.DepotOffre;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import com.yowyob.businesscore.shared.error.ProblemException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GestionOffreService implements GestionOffre {

    private final DepotOffre depot;
    private final RegistreCapacites registre;

    public GestionOffreService(DepotOffre depot, RegistreCapacites registre) {
        this.depot = depot;
        this.registre = registre;
    }

    @Override
    public Mono<DefinitionOffre> declarer(DeclarerOffreCommande commande) {
        UUID offreId = UUID.randomUUID();
        Set<TypeCapacite> demandees = commande.capacites() == null ? Set.of() : commande.capacites();

        List<Capacite> capacites = demandees.stream()
                .map(t -> Capacite.nouvelle(UUID.randomUUID(), offreId, t, true))
                .toList();

        DefinitionOffre offre = DefinitionOffre
                .nouvelle(offreId, commande.versionTypeId(), commande.nom(), commande.formePrix(), commande.prix())
                .avecCapacites(capacites);

        // Persiste, puis active chaque capacité via sa stratégie (STOCKABLE -> stock, etc.).
        return depot.enregistrer(offre)
                .flatMap(enregistree -> activerCapacites(enregistree).thenReturn(enregistree));
    }

    private Mono<Void> activerCapacites(DefinitionOffre offre) {
        return Flux.fromIterable(offre.capacites())
                .filter(Capacite::active)
                .flatMap(c -> registre.pour(c.type())
                        .map(f -> f.activer(offre))
                        .orElseGet(() -> Mono.error(ProblemException.unprocessable(
                                "Capacité non encore supportée : " + c.type()))))
                .then();
    }

    @Override
    public Flux<DefinitionOffre> lister(UUID versionTypeId) {
        return depot.parVersionType(versionTypeId);
    }
}