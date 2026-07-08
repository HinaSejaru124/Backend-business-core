package com.yowyob.businesscore.adapter.out.persistence.developer;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository du compte développeur (socle). Table sans RLS : interrogeable sans tenant établi
 * (l'authentification l'utilise précisément pour déterminer le tenant).
 */
public interface DeveloperAccountRepository extends ReactiveCrudRepository<DeveloperAccountEntity, UUID> {

    Mono<DeveloperAccountEntity> findByEmail(String email);

    Mono<DeveloperAccountEntity> findByKernelTenantId(UUID kernelTenantId);
}
