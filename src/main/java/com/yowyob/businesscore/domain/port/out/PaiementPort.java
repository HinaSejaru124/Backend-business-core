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
 * sur un statut {@code CONFIRME}. Il n'existe qu'une seule implémentation : le paiement réel Kernel
 * (aucune simulation) — toute transaction correspond à un débit mobile money effectif.
 */
public interface PaiementPort {

    /** Ouvre le paiement du passage {@code planActuel} → {@code planCible} pour un développeur. */
    Mono<ResultatPaiement> demanderPaiement(DemandePaiement demande);

    /**
     * Interroge l'issue réelle d'un paiement précédemment ouvert (identifié par sa {@code reference} =
     * id de l'ordre de paiement). Sert à finaliser un paiement mobile money asynchrone.
     */
    Mono<ResultatPaiement> verifierStatut(String reference);

    /**
     * Mode de règlement choisi par le développeur. La passerelle accepte les deux
     * ({@code method} = MOBILE_MONEY | CARD, avec le {@code provider} correspondant) : s'en tenir au
     * mobile money excluait sans raison les développeurs payant par carte.
     */
    enum ModePaiement {
        /** Orange / MTN Money : exige le numéro du payeur. */
        MOBILE_MONEY,
        /** Carte bancaire : le porteur saisit sa carte sur la page sécurisée de la passerelle. */
        CARD;

        public static ModePaiement depuis(String valeur) {
            if (valeur == null || valeur.isBlank()) {
                return MOBILE_MONEY;
            }
            try {
                return valueOf(valeur.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return MOBILE_MONEY;
            }
        }

        public boolean exigePayerReference() {
            return this == MOBILE_MONEY;
        }
    }

    /** {@code payerReference} n'est renseigné que pour le mobile money (numéro du payeur). */
    record DemandePaiement(UUID developerId, String planActuel, String planCible, long montant, String devise,
                           String payerReference, ModePaiement mode) {
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
