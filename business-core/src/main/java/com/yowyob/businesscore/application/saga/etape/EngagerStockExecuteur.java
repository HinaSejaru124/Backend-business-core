package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.Valeurs;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.EtapeCompensable;
import com.yowyob.businesscore.domain.port.internal.ResoudreProduitEntreprise;
import com.yowyob.businesscore.domain.port.out.EngagerStock;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Étape {@code ENGAGER_STOCK} — décrémente le stock kernel après une vente confirmée (mouvement OUT
 * validé). Compensation : mouvement IN de la même quantité.
 */
@Component
public class EngagerStockExecuteur implements EtapeCompensable {

    private final ResoudreProduitEntreprise resoudreProduit;
    private final EngagerStock engagerStock;

    public EngagerStockExecuteur(ResoudreProduitEntreprise resoudreProduit, EngagerStock engagerStock) {
        this.resoudreProduit = resoudreProduit;
        this.engagerStock = engagerStock;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.ENGAGER_STOCK;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        UUID offreId = Valeurs.versUuid(contexte.get(ClesContexte.OFFRE_ID));
        if (offreId == null) {
            return Mono.just(contexte);
        }
        UUID businessId = Valeurs.versUuid(contexte.get(ClesContexte.ENTREPRISE_ID));
        int quantite = Valeurs.versEntierOuDefaut(contexte.get(ClesContexte.QUANTITE), 1);
        UUID commandeId = Valeurs.versUuid(contexte.get(ClesContexte.COMMANDE_ID));
        if (commandeId == null) {
            return Mono.error(ProblemException.unprocessable(
                    "commandeId requis pour ENGAGER_STOCK (exécuter après ENREGISTRER_VENTE)."));
        }
        return resoudreProduit.resoudre(businessId, offreId)
                .flatMap(productId -> engagerStock.sortieVente(productId, quantite, businessId, commandeId)
                        .map(id -> contexte
                                .avec(ClesContexte.PRODUCT_ID, productId)
                                .avec(ClesContexte.MOUVEMENT_STOCK_ID, id)));
    }

    @Override
    public Mono<Void> compenser(ContexteEtape contexte) {
        UUID productId = Valeurs.versUuid(contexte.get(ClesContexte.PRODUCT_ID));
        UUID commandeId = Valeurs.versUuid(contexte.get(ClesContexte.COMMANDE_ID));
        if (productId == null || commandeId == null) {
            return Mono.empty();
        }
        UUID businessId = Valeurs.versUuid(contexte.get(ClesContexte.ENTREPRISE_ID));
        int quantite = Valeurs.versEntierOuDefaut(contexte.get(ClesContexte.QUANTITE), 1);
        return engagerStock.annulerSortie(productId, quantite, businessId, commandeId);
    }
}
