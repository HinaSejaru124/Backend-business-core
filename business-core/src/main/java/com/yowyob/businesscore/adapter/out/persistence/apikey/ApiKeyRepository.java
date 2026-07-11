package com.yowyob.businesscore.adapter.out.persistence.apikey;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository des clés API (socle). Table sans RLS : interrogeable sans tenant établi
 * (l'authentification s'en sert pour résoudre le développeur puis son tenant).
 */
public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKeyEntity, UUID> {

    /** Candidates pour la résolution d'auth : confrontation du secret aux hachés (cf. auth manager). */
    Flux<ApiKeyEntity> findByDeveloperIdAndStatus(UUID developerId, String status);

    Flux<ApiKeyEntity> findByEntrepriseIdAndStatus(UUID entrepriseId, String status);

    Mono<Long> countByDeveloperIdAndStatus(UUID developerId, String status);

    Mono<Long> countByEntrepriseIdAndStatus(UUID entrepriseId, String status);
}
