package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.operation.OperationAvecEtapes;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.operation.spi.ResoudreEntreprise;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Use case — lister / consulter les opérations disponibles pour une entreprise
 * (résolues via sa version épinglée).
 */
@Service
public class ConsulterOperationService {

    private final ResoudreEntreprise resoudreEntreprise;
    private final PersisterOperation persisterOperation;

    public ConsulterOperationService(ResoudreEntreprise resoudreEntreprise,
                                     PersisterOperation persisterOperation) {
        this.resoudreEntreprise = resoudreEntreprise;
        this.persisterOperation = persisterOperation;
    }

    public Flux<OperationAvecEtapes> listerParEntreprise(UUID entrepriseId, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMapMany(entreprise -> persisterOperation.listerParVersion(entreprise.versionTypeId())
                        .flatMap(definition -> persisterOperation.listerEtapes(definition.id())
                                .collectList()
                                .map(etapes -> new OperationAvecEtapes(definition, etapes))));
    }

    public Mono<OperationAvecEtapes> trouverParNom(UUID entrepriseId, String nom, BusinessContext ctx) {
        return resoudreEntreprise.resoudre(entrepriseId)
                .switchIfEmpty(Mono.error(ProblemException.notFound(
                        "Entreprise introuvable : " + entrepriseId)))
                .flatMap(entreprise -> persisterOperation
                        .trouverParVersionEtNom(entreprise.versionTypeId(), nom)
                        .switchIfEmpty(Mono.error(ProblemException.notFound(
                                "Opération introuvable : " + nom)))
                        .flatMap(definition -> persisterOperation.listerEtapes(definition.id())
                                .collectList()
                                .map(etapes -> new OperationAvecEtapes(definition, etapes))));
    }
}
