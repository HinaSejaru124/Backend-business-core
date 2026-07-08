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

    Mono<ApiKeyEntity> findByPrefix(String prefix);

    Flux<ApiKeyEntity> findByDeveloperId(UUID developerId);

    Mono<Long> countByDeveloperIdAndStatus(UUID developerId, String status);
}
