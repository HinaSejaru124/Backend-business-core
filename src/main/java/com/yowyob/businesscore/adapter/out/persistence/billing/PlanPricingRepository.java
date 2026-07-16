package com.yowyob.businesscore.adapter.out.persistence.billing;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/** Repository de la tarification des plans (globale, sans RLS). */
public interface PlanPricingRepository extends ReactiveCrudRepository<PlanPricingEntity, String> {
}
