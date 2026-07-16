package com.yowyob.businesscore.adapter.out.persistence.actor;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ActeurMetierRepository extends ReactiveCrudRepository<ActeurMetierEntity, UUID> {
    Flux<ActeurMetierEntity> findByEntrepriseId(UUID entrepriseId);

    @Query("SELECT COUNT(*) FROM acteur_metier WHERE role_metier_id = :roleMetierId AND valide_jusqua IS NULL")
    Mono<Long> countActifsByRoleMetierId(UUID roleMetierId);
}