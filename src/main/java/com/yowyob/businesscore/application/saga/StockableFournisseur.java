package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.port.internal.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Capacité STOCKABLE — activation au niveau Type/version (l'offre est partagée par toutes les
 * entreprises du type).
 *
 * <p><b>Pas de produit kernel ici</b> : le Product et l'Inventory du kernel sont scopés organisation,
 * inconnue à la déclaration de l'offre. Le produit kernel est donc créé <i>paresseusement par
 * entreprise</i> (où l'organisation est connue) via {@code ResoudreProduitEntreprise}, à la première
 * opération qui en a besoin. L'activation se limite ici à une validation locale : l'offre existe.
 */
@Component
public class StockableFournisseur implements FournisseurDeCapacite {

    private final DepotOffre depotOffre;

    public StockableFournisseur(DepotOffre depotOffre) {
        this.depotOffre = depotOffre;
    }

    @Override
    public TypeCapacite typeSupporte() {
        return TypeCapacite.STOCKABLE;
    }

    @Override
    public Mono<Void> activer(UUID offreId) {
        return depotOffre.parId(offreId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Offre introuvable : " + offreId)))
                .then();
    }
}
