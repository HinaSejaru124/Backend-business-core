package com.yowyob.businesscore.adapter.in.rest.transaction;

import com.yowyob.businesscore.domain.transaction.TransactionVue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Transaction lue du kernel (facture ou commande)")
public record TransactionResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000000") UUID transactionKernelId,
        @Schema(example = "25000.00") BigDecimal montant,
        @Schema(example = "XOF") String devise,
        @Schema(example = "PAYEE") String statut,
        Instant date,
        @Schema(example = "25000.00") BigDecimal montantPaye
) {

    public static TransactionResponse depuis(TransactionVue vue) {
        return new TransactionResponse(
                vue.transactionKernelId(), vue.montant(), vue.devise(), vue.statut(), vue.date(),
                vue.montantPaye());
    }
}
