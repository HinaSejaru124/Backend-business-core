package com.bcaas.core.tenant.application;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.Tenant;
import com.bcaas.core.tenant.domain.model.TenantPlan;
import com.bcaas.core.tenant.domain.model.TenantSettings;
import com.bcaas.core.tenant.port.input.TenantUseCase;
import com.bcaas.core.tenant.port.output.TenantEventPublisher;
import com.bcaas.core.tenant.port.output.TenantRepository;
import reactor.core.publisher.Mono;

/**
 * Implémentation des use cases Tenant.
 *
 * Orchestre le domaine sans contenir de logique métier.
 * La logique métier reste dans l'entité Tenant (Aggregate Root).
 *
 * Flux d'une commande :
 * 1. Charger l'agrégat depuis le repository
 * 2. Déléguer la commande à l'agrégat
 * 3. Persister l'agrégat modifié
 * 4. Publier les événements domaine accumulés
 * 5. Nettoyer les événements
 */
public class TenantService implements TenantUseCase {

    private final TenantRepository tenantRepository;
    private final TenantEventPublisher eventPublisher;

    public TenantService(
            TenantRepository tenantRepository,
            TenantEventPublisher eventPublisher
    ) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Tenant> createTenant(
            String name,
            String slug,
            TenantPlan plan,
            TenantSettings settings,
            BusinessContext context
    ) {
        return tenantRepository.existsBySlug(slug)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException(
                            "Un tenant avec le slug '" + slug + "' existe déjà"
                        ));
                    }
                    Tenant tenant = Tenant.create(
                            name, slug, plan, settings, context.actorId()
                    );
                    return tenantRepository.save(tenant)
                            .flatMap(saved -> publishEventsAndReturn(saved));
                });
    }

    @Override
    public Mono<Tenant> activateTenant(TenantId tenantId, BusinessContext context) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable : " + tenantId
                )))
                .flatMap(tenant -> {
                    tenant.activate(context.actorId());
                    return tenantRepository.save(tenant)
                            .flatMap(saved -> publishEventsAndReturn(saved));
                });
    }

    @Override
    public Mono<Tenant> suspendTenant(
            TenantId tenantId,
            String reason,
            BusinessContext context
    ) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable : " + tenantId
                )))
                .flatMap(tenant -> {
                    tenant.suspend(context.actorId(), reason);
                    return tenantRepository.save(tenant)
                            .flatMap(saved -> publishEventsAndReturn(saved));
                });
    }

    @Override
    public Mono<Tenant> upgradePlan(
            TenantId tenantId,
            TenantPlan newPlan,
            BusinessContext context
    ) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable : " + tenantId
                )))
                .flatMap(tenant -> {
                    tenant.upgradePlan(newPlan, context.actorId());
                    return tenantRepository.save(tenant);
                });
    }

    @Override
    public Mono<Tenant> findById(TenantId tenantId) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable : " + tenantId
                )));
    }

    @Override
    public Mono<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                    "Tenant introuvable avec le slug : " + slug
                )));
    }

    private Mono<Tenant> publishEventsAndReturn(Tenant tenant) {
        return Mono.when(
                tenant.getDomainEvents().stream()
                        .map(eventPublisher::publish)
                        .toList()
        ).doOnSuccess(v -> tenant.clearDomainEvents())
         .thenReturn(tenant);
    }
}
