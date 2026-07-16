package com.yowyob.businesscore.application.usecase.transaction;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.operation.spi.ResoudreEntreprise;
import com.yowyob.businesscore.domain.transaction.TransactionVue;
import com.yowyob.businesscore.domain.transaction.spi.LireTransactions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case — historique des transactions d'une entreprise, <b>lu du kernel</b> à la demande (jamais
 * stocké côté Business Core). Résout l'organisation kernel de l'entreprise puis délègue au port
 * {@link LireTransactions}.
 */
@Service
public class ConsulterTransactionService {

    private final ResoudreEntreprise resoudreEntreprise;
    private final LireTransactions lireTransactions;

    public ConsulterTransactionService(ResoudreEntreprise resoudreEntreprise,
                                       LireTransactions lireTransactions) {
        this.resoudreEntreprise = resoudreEntreprise;
        this.lireTransactions = lireTransactions;
    }

    public Flux<TransactionVue> lister(UUID entrepriseId, int page, int taille, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMapMany(entreprise -> entreprise.organizationId() == null
                        ? Flux.empty()
                        : lireTransactions.listerParOrganisation(entreprise.organizationId(), page, taille));
    }

    public Mono<TransactionVue> trouver(UUID entrepriseId, UUID billId, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMap(entreprise -> entreprise.organizationId() == null
                        ? Mono.error(ProblemException.notFound("Organisation kernel absente."))
                        : lireTransactions.trouverBill(entreprise.organizationId(), billId));
    }

    public Mono<TransactionVue> trouverCommande(UUID entrepriseId, UUID commandeId, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMap(entreprise -> entreprise.organizationId() == null
                        ? Mono.error(ProblemException.notFound("Organisation kernel absente."))
                        : lireTransactions.trouverCommande(entreprise.organizationId(), commandeId));
    }
}
