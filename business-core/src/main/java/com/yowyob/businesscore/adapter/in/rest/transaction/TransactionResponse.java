package com.yowyob.businesscore.adapter.in.rest.transaction;

import com.yowyob.businesscore.domain.transaction.TransactionVue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Réponse d'une transaction lue du kernel (aligné OpenAPI {@code Transaction}).
 */
public record TransactionResponse(
        UUID transactionKernelId,
        BigDecimal montant,
        String devise,
        String statut,
        Instant date,
        BigDecimal montantPaye
) {

    public static TransactionResponse depuis(TransactionVue vue) {
        return new TransactionResponse(
                vue.transactionKernelId(), vue.montant(), vue.devise(), vue.statut(), vue.date(),
                vue.montantPaye());
    }
}
