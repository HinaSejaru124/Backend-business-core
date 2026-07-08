package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.port.internal.PlanificateurDOperation;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Implémentation du port interne {@link PlanificateurDOperation} : fournit la séquence ordonnée
 * d'étapes-types d'une opération déclarée, lue depuis la persistance ({@link PersisterOperation}).
 * Une autre stratégie de planification pourrait coexister sans toucher au moteur.
 */
@Component
public class PlanificateurDOperationParBase implements PlanificateurDOperation {

    private final PersisterOperation persisterOperation;

    public PlanificateurDOperationParBase(PersisterOperation persisterOperation) {
        this.persisterOperation = persisterOperation;
    }

    @Override
    public Flux<TypeEtape> planifier(UUID operationId) {
        return persisterOperation.listerEtapes(operationId).map(etape -> etape.typeEtape());
    }
}
