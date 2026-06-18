package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.FournisseurDeCapaciteDispatcher;
import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GestionOffreService {

    public record DeclarerOffreCommande(
            UUID versionTypeId,
            String nom,
            FormePrix formePrix,
            BigDecimal prix,
            Set<TypeCapacite> capacites) {}

    private final DepotOffre depot;
    private final FournisseurDeCapaciteDispatcher capacites;

    public GestionOffreService(DepotOffre depot, FournisseurDeCapaciteDispatcher capacites) {
        this.depot = depot;
        this.capacites = capacites;
    }

    public Mono<DefinitionOffre> declarer(DeclarerOffreCommande commande) {
        // Construction différée : l'invariant de domaine (prix FIXE) remonte en signal d'erreur réactif.
        return Mono.fromCallable(() -> construire(commande))
                .flatMap(offre -> depot.enregistrer(offre)
                        .flatMap(enregistree -> activerCapacites(enregistree).thenReturn(enregistree)));
    }

    private DefinitionOffre construire(DeclarerOffreCommande commande) {
        UUID offreId = UUID.randomUUID();
        Set<TypeCapacite> demandees = commande.capacites() == null ? Set.of() : commande.capacites();

        List<Capacite> capacites = demandees.stream()
                .map(t -> Capacite.nouvelle(UUID.randomUUID(), offreId, t, true))
                .toList();

        return DefinitionOffre
                .nouvelle(offreId, commande.versionTypeId(), commande.nom(), commande.formePrix(), commande.prix())
                .avecCapacites(capacites);
    }

    private Mono<Void> activerCapacites(DefinitionOffre offre) {
        return Flux.fromIterable(offre.capacites())
                .filter(Capacite::active)
                .flatMap(c -> capacites.activer(c.type(), offre.id()))
                .then();
    }

    public Flux<DefinitionOffre> lister(UUID versionTypeId) {
        return depot.parVersionType(versionTypeId);
    }
}
