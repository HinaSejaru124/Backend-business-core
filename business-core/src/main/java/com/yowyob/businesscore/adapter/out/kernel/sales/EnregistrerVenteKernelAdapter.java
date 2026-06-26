package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.out.EnregistrerVente;
import com.yowyob.businesscore.domain.port.out.VenteEnregistree;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Adapter kernel — implémente la <b>façade financière</b> {@link EnregistrerVente}.
 *
 * <p>Une vente client n'est pas un document payable d'achat : pour produire un bill cashier
 * encaissable, il faut passer par la facture comptable. Un port, plusieurs cores du kernel :
 * <ol>
 *   <li>{@code POST /api/sales/orders} — crée la commande (devise/org/agence résolus par le socle),</li>
 *   <li>{@code POST /api/sales/orders/{orderId}/confirm} — la confirme,</li>
 *   <li>{@code POST /api/accounting/invoices/from-orders/{orderId}} — génère la facture client,</li>
 *   <li>{@code POST /api/bills/import/accounting-invoices/{invoiceId}} — crée le bill cashier (cible
 *       de l'encaissement).</li>
 * </ol>
 * Les identifiants que le métier ignore (organizationId, agencyId, devise) sont fournis par le
 * {@link ResolveurContexteKernel} à partir du {@code businessId} — le domaine reste pur. Tous les
 * appels passent par {@link KernelClient} (auth automatique). Renvoie l'id de commande (point de
 * compensation via {@code /cancel}) et l'id de bill.
 */
@Component
public class EnregistrerVenteKernelAdapter implements EnregistrerVente {

    private final KernelClient kernel;
    private final ResolveurContexteKernel resolveur;

    public EnregistrerVenteKernelAdapter(KernelClient kernel, ResolveurContexteKernel resolveur) {
        this.kernel = kernel;
        this.resolveur = resolveur;
    }

    @Override
    public Mono<VenteEnregistree> enregistrer(UUID offreId, int quantite, UUID beneficiaireId, UUID businessId) {
        return resolveur.resoudre(businessId).flatMap(ctx -> {
            UUID org = ctx.organizationId();
            CreerCommandeRequest requete = new CreerCommandeRequest(
                    ctx.currency(), org, ctx.agencyId(), beneficiaireId, offreId, quantite);
            return kernel.postForOrganization("/api/sales/orders", requete, CommandeResponse.class, org)
                    .flatMap(commande -> kernel
                            .postForOrganization("/api/sales/orders/" + commande.id() + "/confirm",
                                    null, Void.class, org)
                            .then(kernel.postForOrganization(
                                    "/api/accounting/invoices/from-orders/" + commande.id(),
                                    null, FactureClientResponse.class, org))
                            .flatMap(facture -> kernel.postForOrganization(
                                    "/api/bills/import/accounting-invoices/" + facture.id(),
                                    null, BillResponse.class, org))
                            .map(bill -> new VenteEnregistree(
                                    commande.id(), bill.id(), montant(bill), devise(bill, ctx))));
        });
    }

    @Override
    public Mono<Void> annuler(UUID commandeId) {
        // Compensation : le kernel n'a pas de moteur Saga, on annule la commande de vente directement.
        return kernel.post("/api/sales/orders/" + commandeId + "/cancel", null, Void.class).then();
    }

    private static BigDecimal montant(BillResponse bill) {
        return bill.totalAmount() != null ? bill.totalAmount() : bill.amount();
    }

    private static String devise(BillResponse bill, ContexteKernel ctx) {
        return bill.currency() != null ? bill.currency() : ctx.currency();
    }
}

/**
 * Corps d'ouverture de commande sur le core sales du kernel (mappe {@code CreateSalesOrderRequest}) :
 * {@code currency} est obligatoire ; {@code organizationId}/{@code agencyId}/{@code customerThirdPartyId}
 * sont résolus par le socle.
 */
record CreerCommandeRequest(String currency, UUID organizationId, UUID agencyId,
                            UUID customerThirdPartyId, UUID productId, int quantity) {
}

/** Réponse de création/confirmation de commande (on n'en retient que l'identifiant). */
record CommandeResponse(UUID id) {
}

/** Facture client générée depuis la commande ({@code POST /accounting/invoices/from-orders}). */
record FactureClientResponse(UUID id) {
}

/**
 * Bill cashier produit depuis la facture client : sa référence sert de transaction kernel et de cible
 * d'encaissement. Le montant peut être porté par {@code totalAmount} ou {@code amount} selon le core.
 */
record BillResponse(UUID id, BigDecimal totalAmount, BigDecimal amount, String currency) {
}
