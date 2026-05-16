package com.bcaas.core.tenant.port.output;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.Tenant;
import reactor.core.publisher.Mono;

/**
 * Port de sortie — contrat de persistance du Tenant.
 * Le domaine ne sait pas si derrière c'est PostgreSQL, MongoDB ou autre.
 * Analogie réseau : interface de routage abstraite.
 */
public interface TenantRepository {

    Mono<Tenant> save(Tenant tenant);

    Mono<Tenant> findById(TenantId id);

    Mono<Tenant> findBySlug(String slug);

    Mono<Boolean> existsBySlug(String slug);

    Mono<Void> delete(TenantId id);
}
