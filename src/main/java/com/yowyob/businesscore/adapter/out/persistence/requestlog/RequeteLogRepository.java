package com.yowyob.businesscore.adapter.out.persistence.requestlog;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
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

    /** Agrégat dashboard/admin : nombre d'erreurs (statut &gt;= 400) et temps de réponse moyen, sur une fenêtre. */
    record StatsRow(Long nbErreurs, Double dureeMoyenneMs) {
    }

    @Query("""
            SELECT COUNT(*) FILTER (WHERE statut_http >= 400) AS nb_erreurs,
                   AVG(duree_ms) AS duree_moyenne_ms
            FROM requete_log
            WHERE tenant_id = :tenantId AND cree_le >= :depuis
            """)
    Mono<StatsRow> statsParTenant(UUID tenantId, Instant depuis);

    /**
     * Requêtes filtrées (onglet Track). Tous les filtres sont optionnels (null = pas de filtre) :
     * catégorie, méthode HTTP, borne de date (période), et classe de statut via {@code erreurFlag}
     * (null = tous, 1 = échecs 4xx/5xx, 0 = succès &lt; 400). {@code facturableFlag} : null = tous,
     * 1 = facturables uniquement.
     */
    @Query("""
            SELECT * FROM requete_log
            WHERE tenant_id = :tenantId
              AND (:applicationId IS NULL OR entreprise_id = :applicationId)
              AND (:categorie IS NULL OR categorie = :categorie OR (:categorie = 'API' AND categorie <> 'APP'))
              AND (:methode IS NULL OR methode = :methode)
              AND (:depuis IS NULL OR cree_le >= :depuis)
              AND (:erreurFlag IS NULL
                   OR (:erreurFlag = 1 AND statut_http >= 400)
                   OR (:erreurFlag = 0 AND statut_http < 400))
              AND (:facturableFlag IS NULL OR facturable = (:facturableFlag = 1))
            ORDER BY cree_le DESC
            LIMIT :limite OFFSET :decalage
            """)
    Flux<RequeteLogEntity> pageFiltree(UUID tenantId, UUID applicationId, String categorie, String methode,
                                       Instant depuis, Integer erreurFlag, Integer facturableFlag,
                                       int limite, long decalage);

    @Query("""
            SELECT COUNT(*) FROM requete_log
            WHERE tenant_id = :tenantId
              AND (:applicationId IS NULL OR entreprise_id = :applicationId)
              AND (:categorie IS NULL OR categorie = :categorie OR (:categorie = 'API' AND categorie <> 'APP'))
              AND (:methode IS NULL OR methode = :methode)
              AND (:depuis IS NULL OR cree_le >= :depuis)
              AND (:erreurFlag IS NULL
                   OR (:erreurFlag = 1 AND statut_http >= 400)
                   OR (:erreurFlag = 0 AND statut_http < 400))
              AND (:facturableFlag IS NULL OR facturable = (:facturableFlag = 1))
            """)
    Mono<Long> countFiltree(UUID tenantId, UUID applicationId, String categorie, String methode, Instant depuis,
                            Integer erreurFlag, Integer facturableFlag);

    // ─── Agrégats dashboard admin (séries temporelles + top applications) ────────

    /** Une tranche temporelle : instant tronqué (heure/jour/mois) + compteurs réels. */
    record SerieBucket(Instant bucket, Long total, Long facturables, Long erreurs) {
    }

    /**
     * Série temporelle des requêtes d'un tenant, regroupée par {@code unite} (date_trunc :
     * 'hour' | 'day' | 'month'), depuis une borne. Compteurs réels : total, facturables, erreurs.
     */
    @Query("""
            SELECT date_trunc(:unite, cree_le) AS bucket,
                   COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE facturable) AS facturables,
                   COUNT(*) FILTER (WHERE statut_http >= 400) AS erreurs
            FROM requete_log
            WHERE tenant_id = :tenantId AND cree_le >= :depuis
            GROUP BY 1
            ORDER BY 1
            """)
    Flux<SerieBucket> serieParTenant(UUID tenantId, String unite, Instant depuis);

    /** Nombre de requêtes par application (entreprise) d'un tenant, sur une fenêtre. */
    record AppCount(UUID entrepriseId, Long total) {
    }

    @Query("""
            SELECT entreprise_id AS entreprise_id, COUNT(*) AS total
            FROM requete_log
            WHERE tenant_id = :tenantId AND entreprise_id IS NOT NULL AND cree_le >= :depuis
            GROUP BY entreprise_id
            ORDER BY total DESC
            """)
    Flux<AppCount> topAppsParTenant(UUID tenantId, Instant depuis);
}
