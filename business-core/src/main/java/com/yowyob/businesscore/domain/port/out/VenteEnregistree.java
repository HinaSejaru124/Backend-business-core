package com.yowyob.businesscore.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/** Résultat d'un enregistrement de vente : référence de transaction kernel et montant. */
public record VenteEnregistree(UUID transactionKernelId, BigDecimal montant, String devise) {
}
