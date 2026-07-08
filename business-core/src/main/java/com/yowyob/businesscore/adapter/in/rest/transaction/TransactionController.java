package com.yowyob.businesscore.adapter.in.rest.transaction;

import com.yowyob.businesscore.application.common.Page;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.transaction.ConsulterTransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API REST — historique des transactions d'une entreprise (Consultation).
 * {@code GET /v1/businesses/{businessId}/transactions} — données lues du kernel à la demande,
 * jamais dupliquées.
 */
@RestController
@RequestMapping("/v1")
public class TransactionController {

    private final ConsulterTransactionService consulterTransaction;

    public TransactionController(ConsulterTransactionService consulterTransaction) {
        this.consulterTransaction = consulterTransaction;
    }

    @GetMapping("/businesses/{businessId}/transactions")
    public Mono<Page<TransactionResponse>> lister(@PathVariable UUID businessId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return BusinessContextHolder.currentContext()
                .flatMapMany(ctx -> consulterTransaction.lister(businessId, page, size, ctx))
                .map(TransactionResponse::depuis)
                .collectList()
                .map(contenu -> Page.of(contenu, page, size, contenu.size()));
    }

    @GetMapping("/businesses/{businessId}/transactions/{billId}")
    public Mono<TransactionResponse> trouver(@PathVariable UUID businessId, @PathVariable UUID billId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> consulterTransaction.trouver(businessId, billId, ctx))
                .map(TransactionResponse::depuis);
    }

    @GetMapping("/businesses/{businessId}/orders/{orderId}")
    public Mono<TransactionResponse> trouverCommande(@PathVariable UUID businessId,
                                                     @PathVariable UUID orderId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> consulterTransaction.trouverCommande(businessId, orderId, ctx))
                .map(TransactionResponse::depuis);
    }
}
