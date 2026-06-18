package com.yowyob.businesscore.application.capacite;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.internal.offer.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.port.out.offer.GererCatalogueOffre;
import com.yowyob.businesscore.domain.port.out.offer.VerifierDisponibilite;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Capacité STOCKABLE : à l'activation, branche la vérification de stock.
 * On traduit l'offre en Product kernel puis on s'assure que le solde est lisible.
 */
@Component
public class StockableFournisseur implements FournisseurDeCapacite {

    private final GererCatalogueOffre catalogue;
    private final VerifierDisponibilite disponibilite;

    public StockableFournisseur(GererCatalogueOffre catalogue, VerifierDisponibilite disponibilite) {
        this.catalogue = catalogue;
        this.disponibilite = disponibilite;
    }

    @Override
    public TypeCapacite type() {
        return TypeCapacite.STOCKABLE;
    }

    @Override
    public Mono<Void> activer(DefinitionOffre offre) {
        // Traduction offre -> produit avec gestion de stock, puis lecture du solde pour valider le branchement.
        return catalogue.publierCommeProduit(offre)
                .flatMap(disponibilite::soldeDisponible)
                .then();
    }
}