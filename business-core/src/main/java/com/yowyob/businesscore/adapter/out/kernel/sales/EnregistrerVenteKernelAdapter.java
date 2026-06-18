package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.EnregistrerVente;
import com.yowyob.businesscore.domain.port.out.VenteEnregistree;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adapter kernel — implémente la <b>façade financière</b> {@link EnregistrerVente}.
 *
 * <p>Un port, plusieurs cores du kernel (c'est tout l'intérêt de la façade) :
 * <ol>
 *   <li>{@code POST /api/sales/orders} — crée la commande,</li>
 *   <li>{@code POST /api/sales/orders/{orderId}/confirm} — la confirme,</li>
 *   <li>{@code GET /api/cashier/bills/{orderId}} — lit la facture produite (montant, devise).</li>
 * </ol>
 * Tous les appels passent par {@link KernelClient} (auth automatique). Renvoie l'id de commande
 * (point de compensation via {@code /cancel}) et l'id de facture.
 */
@Component
public class EnregistrerVenteKernelAdapter implements EnregistrerVente {

    private final KernelClient kernel;

    public EnregistrerVenteKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Mono<VenteEnregistree> enregistrer(UUID offreId, int quantite, UUID beneficiaireId) {
        CreerCommandeRequest requete = new CreerCommandeRequest(offreId, quantite, beneficiaireId);
        return kernel.post("/api/sales/orders", requete, CommandeResponse.class)
                .flatMap(commande -> kernel
                        .post("/api/sales/orders/" + commande.id() + "/confirm", null, Void.class)
                        .then(kernel.get("/api/cashier/bills/" + commande.id(), FactureResponse.class))
                        .map(facture -> new VenteEnregistree(
                                commande.id(), facture.id(), facture.amount(), facture.currency())));
    }

    @Override
    public Mono<Void> annuler(UUID commandeId) {
        // Compensation : le kernel n'a pas de moteur Saga, on annule la commande de vente directement.
        return kernel.post("/api/sales/orders/" + commandeId + "/cancel", null, Void.class).then();
    }
}

/** Corps d'ouverture de commande sur le core sales du kernel. */
record CreerCommandeRequest(UUID productId, int quantity, UUID customerId) {
}

/** Réponse de création/confirmation de commande (on n'en retient que l'identifiant). */
record CommandeResponse(UUID id) {
}

/** Facture produite côté cashier : sa référence sert de transaction kernel. */
record FactureResponse(UUID id, BigDecimal amount, String currency) {
}
