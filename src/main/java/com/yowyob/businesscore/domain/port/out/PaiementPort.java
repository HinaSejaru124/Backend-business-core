package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de paiement d'un changement de plan.
 *
 * <p>Le paiement réel est fourni par <b>Kernel Core</b> via sa passerelle d'encaissement mobile money
 * (MyCoolPay). L'implémentation active est {@code KernelPaiementAdapter} : le paiement est
 * <b>asynchrone</b> — {@link #demanderPaiement} ouvre un ordre de paiement (statut {@code EN_ATTENTE} +
 * {@code urlPaiement}) et {@link #verifierStatut} interroge le kernel pour connaître l'issue réelle après
 * que le payeur a validé sur son téléphone. La logique métier ({@code PlanService}) n'active le plan que
 * sur un statut {@code CONFIRME}. {@code SimulationPaiementAdapter} reste disponible (non câblé) pour les
 * tests / un éventuel repli.
 */
public interface PaiementPort {

    /** Ouvre le paiement du passage {@code planActuel} → {@code planCible} pour un développeur. */
    Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande);

    /**
     * Interroge l'issue réelle d'un paiement précédemment ouvert (identifié par sa {@code reference} =
     * id de l'ordre de paiement). Sert à finaliser un paiement mobile money asynchrone.
     */
    Mono<ResultatPaiement> verifierStatut(String reference);

    record DemandePaiement(UUID developerId, String planActuel, String planCible, long montant, String devise,
                           String payerReference) {
    }

    /**
     * Issue du paiement. {@code CONFIRME} : plan activable ({@code urlPaiement} nul).
     * {@code EN_ATTENTE} : paiement à finaliser via {@code urlPaiement} (redirection MyCoolPay) puis à
     * confirmer via {@link #verifierStatut}. {@code REFUSE} : refusé/annulé.
     */
    record ResultatPaiement(Statut statut, String urlPaiement, String reference) {
        public enum Statut { CONFIRME, EN_ATTENTE, REFUSE }
    }
}
