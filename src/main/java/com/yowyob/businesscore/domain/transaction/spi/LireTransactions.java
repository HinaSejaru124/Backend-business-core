package com.yowyob.businesscore.domain.transaction.spi;

import com.yowyob.businesscore.domain.transaction.TransactionVue;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie (feature Transactions) — lit l'historique des transactions <b>depuis le kernel</b>
 * (jamais stocké côté Business Core). L'adapter kernel l'implémente via {@code KernelClient}.
 */
public interface LireTransactions {

    Flux<TransactionVue> listerParOrganisation(UUID organizationId, int page, int taille);

    Mono<TransactionVue> trouverBill(UUID organizationId, UUID billId);

    Mono<TransactionVue> trouverCommande(UUID organizationId, UUID commandeId);
}
