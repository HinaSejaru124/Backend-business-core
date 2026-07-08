// adapter/out/persistence/rule/RegleMetierRepository.java
package com.yowyob.businesscore.adapter.out.persistence.rule;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface RegleMetierRepository
        extends ReactiveCrudRepository<RegleMetierEntity, UUID> {

    // Règles de Type d'une version précise pour un déclencheur
    Flux<RegleMetierEntity> findByDeclencheurAndVersionTypeId(String declencheur, UUID versionTypeId);

    // Règles locales d'une entreprise pour un déclencheur
    Flux<RegleMetierEntity> findByDeclencheurAndEntrepriseId(String declencheur, UUID entrepriseId);

    Flux<RegleMetierEntity> findByVersionTypeId(UUID versionTypeId);

    Flux<RegleMetierEntity> findByEntrepriseId(UUID entrepriseId);
}