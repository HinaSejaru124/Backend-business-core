package com.bcaas.core.infrastructure.persistence.tenant;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.Tenant;
import com.bcaas.core.tenant.port.output.TenantRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapteur PostgreSQL pour le port TenantRepository.
 *
 * Implémente le port de sortie défini par le domaine.
 * Traduit entre les entités R2DBC et les agrégats du domaine.
 * Le domaine ne sait pas que PostgreSQL existe.
 *
 * Analogie réseau : couche liaison de données —
 * traduit les données du format domaine au format réseau (DB).
 */
@Component
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantR2dbcRepository r2dbcRepository;

    public TenantRepositoryAdapter(TenantR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Tenant> save(Tenant tenant) {
        return r2dbcRepository.save(TenantEntity.fromDomain(tenant))
                .map(TenantEntity::toDomain);
    }

    @Override
    public Mono<Tenant> findById(TenantId id) {
        return r2dbcRepository.findById(id.value())
                .map(TenantEntity::toDomain);
    }

    @Override
    public Mono<Tenant> findBySlug(String slug) {
        return r2dbcRepository.findBySlug(slug)
                .map(TenantEntity::toDomain);
    }

    @Override
    public Mono<Boolean> existsBySlug(String slug) {
        return r2dbcRepository.existsBySlug(slug);
    }

    @Override
    public Mono<Void> delete(TenantId id) {
        return r2dbcRepository.deleteById(id.value());
    }
}
