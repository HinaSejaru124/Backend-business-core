package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.transaction.TransactionVue;
import com.yowyob.businesscore.domain.transaction.spi.LireTransactions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Adapter kernel — implémente {@link LireTransactions}. Lit l'historique des transactions d'une
 * organisation depuis le core cashier ({@code GET /api/cashier/transactions}) via la variante
 * {@code getForOrganization} (ajoute {@code X-Organization-Id}). Les données ne sont jamais stockées.
 */
@Component
public class LireTransactionsKernelAdapter implements LireTransactions {

    private final KernelClient kernel;

    public LireTransactionsKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Flux<TransactionVue> listerParOrganisation(UUID organizationId, int page, int taille) {
        String path = "/api/cashier/transactions?page=" + page + "&size=" + taille;
        return kernel.getForOrganization(path, TransactionItem[].class, organizationId)
                .flatMapMany(Flux::fromArray)
                .map(item -> new TransactionVue(
                        item.id(), item.amount(), item.currency(), item.status(), item.date()));
    }
}

/** Élément de transaction tel que renvoyé par le core cashier du kernel. */
record TransactionItem(UUID id, BigDecimal amount, String currency, String status, Instant date) {
}
