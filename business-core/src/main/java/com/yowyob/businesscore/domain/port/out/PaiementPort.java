package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de paiement d'un changement de plan.
 *
 * <p>Le paiement réel est fourni par <b>Kernel Core</b> (indisponible pour l'instant). En attendant, un
 * adapter de simulation confirme immédiatement, ce qui permet de tester l'activation et le déblocage du
 * quota de bout en bout. Le jour où l'API de paiement Kernel existe, il suffit d'ajouter un
 * {@code KernelPaiementAdapter} implémentant ce port — la logique métier ({@code PlanService}) ne change
 * pas.
 */
public interface PaiementPort {

    /** Demande le paiement du passage {@code planActuel} → {@code planCible} pour un développeur. */
    Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande);

    record DemandePaiement(UUID developerId, String planActuel, String planCible, long montant, String devise) {
    }

    /**
     * Issue du paiement. {@code CONFIRME} : plan activable tout de suite ({@code urlPaiement} nul).
     * {@code EN_ATTENTE} : paiement à finaliser via {@code urlPaiement} (cas Kernel réel à venir).
     * {@code REFUSE} : refusé.
     */
    record ResultatPaiement(Statut statut, String urlPaiement, String reference) {
        public enum Statut { CONFIRME, EN_ATTENTE, REFUSE }
    }
}
