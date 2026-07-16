package com.yowyob.businesscore.adapter.out.persistence.operation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Repository des étapes d'opération, renvoyées dans l'ordre de la séquence. RLS garantit l'isolation.
 */
public interface EtapeOperationRepository
        extends ReactiveCrudRepository<EtapeOperationEntity, UUID> {

    Flux<EtapeOperationEntity> findByOperationIdOrderByOrdreAsc(UUID operationId);
}
