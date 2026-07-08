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
 * <p><b>Session de caisse</b> : la session ouverte de la caisse est résolue (ou ouverte) via
 * {@link SessionCaisseKernelSupport} avant le paiement ; sans caisse/caissier connus, le paiement
 * part sans {@code sessionId} (session par défaut côté kernel).
 */
@Component
public class PorteMonnaieMonetaireAdapter implements PorteMonnaieGenerique {

    private final KernelClient kernel;
    private final ResolveurContexteKernel resolveur;
    private final SessionCaisseKernelSupport sessionCaisse;

    public PorteMonnaieMonetaireAdapter(KernelClient kernel, ResolveurContexteKernel resolveur,
                                        SessionCaisseKernelSupport sessionCaisse) {
        this.kernel = kernel;
        this.resolveur = resolveur;
        this.sessionCaisse = sessionCaisse;
    }

    @Override
    public Mono<UUID> enregistrerEchange(UUID billId, BigDecimal montant, String devise, UUID businessId) {
        if (billId == null) {
            return Mono.error(ProblemException.unprocessable(
                    "Encaissement impossible : aucun bill cashier à régler."));
        }
        return resolveur.resoudre(businessId).flatMap(ctx ->
                sessionCaisse.resoudreOuOuvrir(ctx).flatMap(sessionId -> {
                    String path = "/api/bills/pay?billId=" + billId;
                    PayBillRequest requete = new PayBillRequest(
                            montant, ctx.registerId(), sessionId.orElse(null));
                    return kernel.postForOrganization(path, requete, PaiementResponse.class,
                                    ctx.organizationId())
                            .map(paiement -> paiement.id());
                }));
    }
}

/**
 * Corps de paiement d'un bill cashier (mappe {@code PayBillRequest}) : {@code amount} et
 * {@code registerId} ; {@code sessionId} optionnel (null = session ouverte par défaut de la caisse).
 */
record PayBillRequest(BigDecimal amount, UUID registerId, UUID sessionId) {
}

/** Paiement créé côté kernel (mappe {@code PaymentView}) : on n'en retient que l'identifiant. */
record PaiementResponse(UUID id) {
}
