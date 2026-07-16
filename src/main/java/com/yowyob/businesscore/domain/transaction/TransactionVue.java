package com.yowyob.businesscore.domain.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Brique 6 — vue d'une transaction (échange de valeur) <b>lue du kernel</b>, jamais stockée par le
 * Business Core (RG : on ne duplique pas les données opérationnelles du kernel). Projection minimale
 * renvoyée par l'endpoint d'historique des transactions.
 */
public record TransactionVue(
        UUID transactionKernelId,
        BigDecimal montant,
        String devise,
        String statut,
        Instant date,
        BigDecimal montantPaye
) {
}
