package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.port.internal.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.port.out.GererCatalogueOffre;
import com.yowyob.businesscore.domain.port.out.VerifierDisponibilite;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Capacité STOCKABLE : à l'activation, traduit l'offre en produit kernel avec gestion de stock,
 * puis vérifie que le solde est lisible. L'offre est rechargée depuis le dépôt local.
 */
@Component
public class StockableFournisseur implements FournisseurDeCapacite {

    private final DepotOffre depotOffre;
    private final GererCatalogueOffre catalogue;
    private final VerifierDisponibilite disponibilite;

    public StockableFournisseur(DepotOffre depotOffre,
                                GererCatalogueOffre catalogue,
                                VerifierDisponibilite disponibilite) {
        this.depotOffre = depotOffre;
        this.catalogue = catalogue;
        this.disponibilite = disponibilite;
    }

    @Override
    public TypeCapacite typeSupporte() {
        return TypeCapacite.STOCKABLE;
    }

    @Override
    public Mono<Void> activer(UUID offreId) {
        return depotOffre.parId(offreId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Offre introuvable : " + offreId)))
                .flatMap(offre -> catalogue.creerProduit(
                        offre.nom(), offre.possede(TypeCapacite.STOCKABLE), prixDe(offre)))
                .flatMap(disponibilite::soldeStock)
                .then();
    }

    private static java.math.BigDecimal prixDe(DefinitionOffre offre) {
        return offre.formePrix() == FormePrix.FIXE ? offre.prix() : null;
    }
}
