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

    /**
     * Requêtes filtrées (onglet Track). Tous les filtres sont optionnels (null = pas de filtre) :
     * catégorie, méthode HTTP, borne de date (période), et classe de statut via {@code erreurFlag}
     * (null = tous, 1 = échecs 4xx/5xx, 0 = succès &lt; 400). {@code facturableFlag} : null = tous,
     * 1 = facturables uniquement.
     */
    @Query("""
            SELECT * FROM requete_log
            WHERE tenant_id = :tenantId
              AND (:categorie IS NULL OR categorie = :categorie)
              AND (:methode IS NULL OR methode = :methode)
              AND (:depuis IS NULL OR cree_le >= :depuis)
              AND (:erreurFlag IS NULL
                   OR (:erreurFlag = 1 AND statut_http >= 400)
                   OR (:erreurFlag = 0 AND statut_http < 400))
              AND (:facturableFlag IS NULL OR facturable = (:facturableFlag = 1))
            ORDER BY cree_le DESC
            LIMIT :limite OFFSET :decalage
            """)
    Flux<RequeteLogEntity> pageFiltree(UUID tenantId, String categorie, String methode, Instant depuis,
                                       Integer erreurFlag, Integer facturableFlag, int limite, long decalage);

    @Query("""
            SELECT COUNT(*) FROM requete_log
            WHERE tenant_id = :tenantId
              AND (:categorie IS NULL OR categorie = :categorie)
              AND (:methode IS NULL OR methode = :methode)
              AND (:depuis IS NULL OR cree_le >= :depuis)
              AND (:erreurFlag IS NULL
                   OR (:erreurFlag = 1 AND statut_http >= 400)
                   OR (:erreurFlag = 0 AND statut_http < 400))
              AND (:facturableFlag IS NULL OR facturable = (:facturableFlag = 1))
            """)
    Mono<Long> countFiltree(UUID tenantId, String categorie, String methode, Instant depuis,
                            Integer erreurFlag, Integer facturableFlag);
}
