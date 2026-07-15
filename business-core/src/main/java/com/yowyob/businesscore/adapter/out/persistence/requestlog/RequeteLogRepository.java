package com.yowyob.businesscore.adapter.out.persistence.requestlog;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Repository du journal détaillé des requêtes. RLS garantit l'isolation par tenant. */
public interface RequeteLogRepository extends ReactiveCrudRepository<RequeteLogEntity, UUID> {

    @Query("""
            SELECT * FROM requete_log
            WHERE tenant_id = :tenantId
            ORDER BY cree_le DESC
            LIMIT :limite OFFSET :decalage
            """)
    Flux<RequeteLogEntity> pageParTenant(UUID tenantId, int limite, long decalage);

    @Query("""
            SELECT * FROM requete_log
            WHERE tenant_id = :tenantId AND categorie = :categorie
            ORDER BY cree_le DESC
            LIMIT :limite OFFSET :decalage
            """)
    Flux<RequeteLogEntity> pageParTenantEtCategorie(UUID tenantId, String categorie, int limite, long decalage);

    Mono<Long> countByTenantId(UUID tenantId);

    Mono<Long> countByTenantIdAndCategorie(UUID tenantId, String categorie);
}
