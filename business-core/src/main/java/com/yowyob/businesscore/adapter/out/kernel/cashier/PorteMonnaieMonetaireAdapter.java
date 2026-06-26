package com.yowyob.businesscore.adapter.out.kernel.cashier;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.PorteMonnaieGenerique;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stratégie (port interne) {@link PorteMonnaieGenerique} — implémentation <b>monétaire</b> de départ.
 *
 * <p>Le paiement d'une vente client s'enregistre sur le bill cashier produit par la vente :
 * {@code POST /api/bills/pay} avec le montant, la caisse ({@code registerId}) et, optionnellement, la
 * session de caisse. La caisse et l'organisation sont fournies par le {@link ResolveurContexteKernel}
 * à partir du {@code businessId} — le métier ne les connaît pas. Le monétaire n'est qu'une forme
 * d'échange ; troc/don pourront suivre comme d'autres implémentations sans toucher au domaine.
 *
 * <p><b>Session de caisse</b> : option A retenue au départ (on ne force pas de {@code sessionId}, le
 * kernel rattache la session ouverte de la caisse). Pour passer à une gestion explicite de session
 * (résoudre/ouvrir la session via {@code GET/POST /api/sessions}), il suffit de renseigner {@code sessionId}
 * ci-dessous sans changer le port.
 */
@Component
public class PorteMonnaieMonetaireAdapter implements PorteMonnaieGenerique {

    private final KernelClient kernel;
    private final ResolveurContexteKernel resolveur;

    public PorteMonnaieMonetaireAdapter(KernelClient kernel, ResolveurContexteKernel resolveur) {
        this.kernel = kernel;
        this.resolveur = resolveur;
    }

    @Override
    public Mono<UUID> enregistrerEchange(UUID billId, BigDecimal montant, String devise, UUID businessId) {
        if (billId == null) {
            return Mono.error(ProblemException.unprocessable(
                    "Encaissement impossible : aucun bill cashier à régler."));
        }
        return resolveur.resoudre(businessId).flatMap(ctx -> {
            PayBillRequest requete = new PayBillRequest(billId, montant, ctx.registerId(), null);
            return kernel.postForOrganization("/api/bills/pay", requete, PaiementResponse.class,
                            ctx.organizationId())
                    .map(PaiementResponse::id);
        });
    }
}

/**
 * Corps de paiement d'un bill cashier (mappe {@code PayBillRequest}) : {@code amount} et
 * {@code registerId} ; {@code sessionId} optionnel (null = session ouverte par défaut de la caisse).
 */
record PayBillRequest(UUID billId, BigDecimal amount, UUID registerId, UUID sessionId) {
}

/** Paiement créé côté kernel (mappe {@code PaymentView}) : on n'en retient que l'identifiant. */
record PaiementResponse(UUID id) {
}
