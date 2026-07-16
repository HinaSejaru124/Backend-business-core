package com.yowyob.businesscore.adapter.out.persistence.billing;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Repository des demandes de changement de plan (audit facturation). Table sans RLS.
 */
public interface PlanChangeRequestRepository extends ReactiveCrudRepository<PlanChangeRequestEntity, UUID> {

    Flux<PlanChangeRequestEntity> findByDeveloperIdOrderByCreatedAtDesc(UUID developerId);
}
