package com.yowyob.businesscore.adapter.out.persistence.operation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Repository des définitions d'opération. Le filtrage tenant est garanti par la RLS (jamais de
 * {@code WHERE tenant_id}).
 */
public interface DefinitionOperationRepository
        extends ReactiveCrudRepository<DefinitionOperationEntity, UUID> {

    Flux<DefinitionOperationEntity> findByVersionTypeId(UUID versionTypeId);
}
