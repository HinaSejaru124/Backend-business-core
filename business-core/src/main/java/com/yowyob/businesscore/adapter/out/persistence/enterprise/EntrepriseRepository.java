package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository des entreprises. RLS garantit l'isolation : {@code findAll()} ne renvoie que les
 * entreprises du tenant courant.
 */
public interface EntrepriseRepository
        extends ReactiveCrudRepository<EntrepriseEntity, UUID> {

    Mono<Long> countByTenantId(UUID tenantId);
}
