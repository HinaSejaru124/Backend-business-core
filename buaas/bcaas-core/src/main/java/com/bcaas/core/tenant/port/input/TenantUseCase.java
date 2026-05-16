package com.bcaas.core.tenant.port.input;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.Tenant;
import com.bcaas.core.tenant.domain.model.TenantPlan;
import com.bcaas.core.tenant.domain.model.TenantSettings;
import reactor.core.publisher.Mono;

/**
 * Port d'entrée — contrat des use cases Tenant.
 * Ce que le monde extérieur (REST, Kafka, scheduler) peut demander
 * au domaine Tenant. Rien de plus, rien de moins.
 */
public interface TenantUseCase {

    Mono<Tenant> createTenant(
            String name,
            String slug,
            TenantPlan plan,
            TenantSettings settings,
            BusinessContext context
    );

    Mono<Tenant> activateTenant(TenantId tenantId, BusinessContext context);

    Mono<Tenant> suspendTenant(TenantId tenantId, String reason, BusinessContext context);

    Mono<Tenant> upgradePlan(TenantId tenantId, TenantPlan newPlan, BusinessContext context);

    Mono<Tenant> findById(TenantId tenantId);

    Mono<Tenant> findBySlug(String slug);
}
