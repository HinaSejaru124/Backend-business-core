package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Repository R2DBC pour parametre_config. RLS garantit l'isolation tenant. */
public interface ParametreConfigRepository
        extends ReactiveCrudRepository<ParametreConfigEntity, UUID> {

    Mono<ParametreConfigEntity> findByCleAndVersionTypeId(String cle, UUID versionTypeId);

    Mono<ParametreConfigEntity> findByCleAndEntrepriseId(String cle, UUID entrepriseId);

    Flux<ParametreConfigEntity> findAllByVersionTypeId(UUID versionTypeId);

    Flux<ParametreConfigEntity> findAllByEntrepriseId(UUID entrepriseId);
}
