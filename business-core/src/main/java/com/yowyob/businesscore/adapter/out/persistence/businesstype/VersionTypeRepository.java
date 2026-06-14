package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Repository des versions de type (tenant-filtré par RLS). */
public interface VersionTypeRepository extends ReactiveCrudRepository<VersionTypeEntity, UUID> {

    Flux<VersionTypeEntity> findByTypeMetierIdOrderByNumeroAsc(UUID typeMetierId);

    Mono<VersionTypeEntity> findByTypeMetierIdAndNumero(UUID typeMetierId, int numero);
}
