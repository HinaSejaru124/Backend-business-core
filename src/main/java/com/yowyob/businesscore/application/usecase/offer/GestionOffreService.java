package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.FournisseurDeCapaciteDispatcher;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotProduitEntreprise;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
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

    public record ModifierOffreCommande(
            UUID versionTypeId,
            UUID offreId,
            String nom,
            FormePrix formePrix,
            BigDecimal prix,
            Set<TypeCapacite> capacites) {}

    private final DepotOffre depot;
    private final DepotProduitEntreprise depotProduit;
    private final FournisseurDeCapaciteDispatcher capacites;
    private final PersisterVersionType persisterVersionType;

    public GestionOffreService(DepotOffre depot,
                               DepotProduitEntreprise depotProduit,
                               FournisseurDeCapaciteDispatcher capacites,
                               PersisterVersionType persisterVersionType) {
        this.depot = depot;
        this.depotProduit = depotProduit;
        this.capacites = capacites;
        this.persisterVersionType = persisterVersionType;
    }

    /** RG-03 : une version publiée (immuable) ne peut plus voir ses offres créées/modifiées/supprimées. */
    private Mono<Void> verifierVersionModifiable(UUID versionTypeId) {
        return persisterVersionType.trouverParId(versionTypeId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Version introuvable : " + versionTypeId)))
                .doOnNext(VersionType::verifierModifiable)
                .then();
    }

    public Mono<DefinitionOffre> declarer(DeclarerOffreCommande commande) {
        return verifierVersionModifiable(commande.versionTypeId())
                .then(Mono.fromCallable(() -> construire(commande.versionTypeId(), UUID.randomUUID(),
                        commande.nom(), commande.formePrix(), commande.prix(), commande.capacites())))
                .flatMap(offre -> depot.enregistrer(offre)
                        .flatMap(enregistree -> activerCapacites(enregistree).thenReturn(enregistree)));
    }

    public Flux<DefinitionOffre> lister(UUID versionTypeId) {
        return depot.parVersionType(versionTypeId);
    }

    public Mono<DefinitionOffre> trouver(UUID versionTypeId, UUID offreId) {
        return depot.parId(offreId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Offre introuvable : " + offreId)))
                .filter(o -> o.versionTypeId().equals(versionTypeId))
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Offre " + offreId + " absente de cette version")));
    }

    public Mono<DefinitionOffre> modifier(ModifierOffreCommande commande) {
        return verifierVersionModifiable(commande.versionTypeId())
                .then(trouver(commande.versionTypeId(), commande.offreId()))
                .flatMap(existante -> Mono.fromCallable(() -> construire(
                                existante.versionTypeId(), existante.id(),
                                commande.nom(), commande.formePrix(), commande.prix(), commande.capacites()))
                        .flatMap(depot::enregistrer));
    }

    public Mono<Void> supprimer(UUID versionTypeId, UUID offreId) {
        return verifierVersionModifiable(versionTypeId)
                .then(trouver(versionTypeId, offreId))
                .flatMap(offre -> depotProduit.existeMappingPourOffre(offre.id())
                        .flatMap(existe -> {
                            if (Boolean.TRUE.equals(existe)) {
                                return Mono.error(ProblemException.conflict(
                                        "Impossible de supprimer l'offre : un produit kernel est déjà mappé.")
                                        .violatedRule("OFFRE_MAPPEE_PRODUIT"));
                            }
                            return depot.supprimer(offre.id());
                        }));
    }

    private DefinitionOffre construire(UUID versionTypeId, UUID offreId, String nom,
                                       FormePrix formePrix, BigDecimal prix,
                                       Set<TypeCapacite> capacitesDemandees) {
        Set<TypeCapacite> demandees = capacitesDemandees == null ? Set.of() : capacitesDemandees;
        List<Capacite> caps = demandees.stream()
                .map(t -> Capacite.nouvelle(UUID.randomUUID(), offreId, t, true))
                .toList();
        return DefinitionOffre.nouvelle(offreId, versionTypeId, nom, formePrix, prix)
                .avecCapacites(caps);
    }

    private Mono<Void> activerCapacites(DefinitionOffre offre) {
        return Flux.fromIterable(offre.capacites())
                .filter(c -> c.active())
                .flatMap(c -> capacites.activer(c.type(), offre.id()))
                .then();
    }
}
