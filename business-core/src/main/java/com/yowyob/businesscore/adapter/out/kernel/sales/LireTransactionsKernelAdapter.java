package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.transaction.TransactionVue;
import com.yowyob.businesscore.domain.transaction.spi.LireTransactions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Adapter kernel — implémente {@link LireTransactions}. Lit l'historique des mouvements de caisse
 * ({@code GET /api/cashier/movements}) et le détail d'un bill ({@code GET /api/cashier/bills/{id}}).
 */
@Component
public class LireTransactionsKernelAdapter implements LireTransactions {

    private final KernelClient kernel;

    public LireTransactionsKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Flux<TransactionVue> listerParOrganisation(UUID organizationId, int page, int taille) {
        return kernel.getForOrganization("/api/cashier/movements", CashMovementView[].class, organizationId)
                .flatMapMany(Flux::fromArray)
                .skip((long) page * taille)
                .take(taille)
                .map(this::versVue);
    }

    @Override
    public Mono<TransactionVue> trouverBill(UUID organizationId, UUID billId) {
        return kernel.getForOrganization(
                        "/api/cashier/bills/" + billId, BillView.class, organizationId)
                .map(this::versVue);
    }

    @Override
    public Mono<TransactionVue> trouverCommande(UUID organizationId, UUID commandeId) {
        return kernel.getForOrganization(
                        "/api/sales/orders/" + commandeId, SalesOrderView.class, organizationId)
                .map(this::versVue);
    }

    private TransactionVue versVue(CashMovementView item) {
        return new TransactionVue(
                item.id(), item.amount(), item.currency(), item.status(), item.createdAt(), null);
    }

    private TransactionVue versVue(BillView bill) {
        return new TransactionVue(
                bill.id(), bill.totalAmount(), bill.currency(), bill.status(), bill.createdAt(),
                bill.paidAmount());
    }

    private TransactionVue versVue(SalesOrderView order) {
        return new TransactionVue(
                order.id(), order.unitPrice(), order.currency(), order.status(), null, null);
    }
}

record CashMovementView(UUID id, BigDecimal amount, String currency, String status, Instant createdAt) {
}

record BillView(UUID id, BigDecimal totalAmount, BigDecimal paidAmount, String currency, String status,
                Instant createdAt) {
}

record SalesOrderView(UUID id, BigDecimal unitPrice, String currency, String status) {
}
