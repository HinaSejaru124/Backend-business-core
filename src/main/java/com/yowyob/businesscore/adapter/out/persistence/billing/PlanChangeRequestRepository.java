package com.yowyob.businesscore.adapter.out.persistence.billing;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository des demandes de changement de plan (audit facturation). Table sans RLS.
 */
public interface PlanChangeRequestRepository extends ReactiveCrudRepository<PlanChangeRequestEntity, UUID> {

    Flux<PlanChangeRequestEntity> findByDeveloperIdOrderByCreatedAtDesc(UUID developerId);

    /** Dernière demande dans un statut donné (ex. {@code EN_ATTENTE}) pour finaliser un paiement. */
    Mono<PlanChangeRequestEntity> findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(UUID developerId, String statut);
}
