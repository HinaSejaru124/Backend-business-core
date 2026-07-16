package com.yowyob.businesscore.adapter.out.persistence.trace;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Repository des traces d'opération. RLS garantit l'isolation ; {@code cle_idempotence} est unique
 * par tenant (un rejeu retrouve la trace existante au lieu de créer un doublon).
 */
public interface TraceOperationRepository
        extends ReactiveCrudRepository<TraceOperationEntity, UUID> {

    Mono<TraceOperationEntity> findByCleIdempotence(String cleIdempotence);

    Flux<TraceOperationEntity> findByEntrepriseId(UUID entrepriseId);

    /** Opération la plus exécutée sur la fenêtre (dashboard — "top opérations"). */
    record TopOperationRow(String operationNom, Long total) {
    }

    @Query("""
            SELECT operation_nom, COUNT(*) AS total
            FROM trace_operation
            WHERE tenant_id = :tenantId AND cree_le >= :depuis
            GROUP BY operation_nom
            ORDER BY total DESC
            LIMIT :limite
            """)
    Flux<TopOperationRow> topOperations(UUID tenantId, Instant depuis, int limite);

    /** Entreprise la plus active sur la fenêtre (dashboard — "top entreprises"). */
    record TopEntrepriseRow(UUID entrepriseId, String nom, Long total) {
    }

    @Query("""
            SELECT t.entreprise_id AS entreprise_id, e.nom AS nom, COUNT(*) AS total
            FROM trace_operation t JOIN entreprise e ON e.id = t.entreprise_id
            WHERE t.tenant_id = :tenantId AND t.cree_le >= :depuis
            GROUP BY t.entreprise_id, e.nom
            ORDER BY total DESC
            LIMIT :limite
            """)
    Flux<TopEntrepriseRow> topEntreprises(UUID tenantId, Instant depuis, int limite);

    /** Dernières exécutions d'opération, toutes entreprises confondues (dashboard — "activité récente"). */
    record ActiviteRow(UUID entrepriseId, String entrepriseNom, String operationNom, String statut, Instant creeLe) {
    }

    @Query("""
            SELECT t.entreprise_id AS entreprise_id, e.nom AS entreprise_nom,
                   t.operation_nom AS operation_nom, t.statut AS statut, t.cree_le AS cree_le
            FROM trace_operation t JOIN entreprise e ON e.id = t.entreprise_id
            WHERE t.tenant_id = :tenantId
            ORDER BY t.cree_le DESC
            LIMIT :limite
            """)
    Flux<ActiviteRow> activiteRecente(UUID tenantId, int limite);
}
