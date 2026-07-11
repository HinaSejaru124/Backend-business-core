package com.yowyob.businesscore.adapter.in.rest.transaction;

import com.yowyob.businesscore.application.common.Page;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.transaction.ConsulterTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Consultation", description = "Transactions et traces")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1")
public class TransactionController {

    private final ConsulterTransactionService consulterTransaction;

    public TransactionController(ConsulterTransactionService consulterTransaction) {
        this.consulterTransaction = consulterTransaction;
    }

    @Operation(summary = "Lister les transactions",
            description = "Historique paginé des transactions kernel de l'entreprise (lecture à la demande).")
    @ApiResponse(responseCode = "200", description = "Page de transactions")
    @GetMapping("/businesses/{businessId}/transactions")
    public Mono<Page<TransactionResponse>> lister(
            @PathVariable UUID businessId,
            @Parameter(description = "Numéro de page (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
                .flatMapMany(ctx -> consulterTransaction.lister(businessId, page, size, ctx))
                .map(TransactionResponse::depuis)
                .collectList()
                .map(contenu -> Page.of(contenu, page, size, contenu.size()));
    }

    @Operation(summary = "Consulter une transaction (facture)",
            description = "Détail d'une facture kernel par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La transaction"),
            @ApiResponse(responseCode = "404", description = "Transaction introuvable")
    })
    @GetMapping("/businesses/{businessId}/transactions/{billId}")
    public Mono<TransactionResponse> trouver(@PathVariable UUID businessId, @PathVariable UUID billId) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
                .flatMap(ctx -> consulterTransaction.trouver(businessId, billId, ctx))
                .map(TransactionResponse::depuis);
    }

    @Operation(summary = "Consulter une commande",
            description = "Détail d'une commande kernel par son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "La commande"),
            @ApiResponse(responseCode = "404", description = "Commande introuvable")
    })
    @GetMapping("/businesses/{businessId}/orders/{orderId}")
    public Mono<TransactionResponse> trouverCommande(@PathVariable UUID businessId,
                                                     @PathVariable UUID orderId) {
        return BusinessContextHolder.currentContext()
                .doOnNext(ctx -> ctx.verifierAcces(businessId))
                .flatMap(ctx -> consulterTransaction.trouverCommande(businessId, orderId, ctx))
                .map(TransactionResponse::depuis);
    }
}
