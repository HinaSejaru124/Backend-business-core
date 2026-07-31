package com.yowyob.businesscore.adapter.out.persistence.apikey;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

/**
 * Repository des agrégats quotidiens d'usage. L'{@link #upsert} écrit une valeur absolue par (clé, jour)
 * — idempotent : le flush réécrit le compteur courant sans risque de double comptage.
 */
public interface ApiKeyUsageDailyRepository extends ReactiveCrudRepository<ApiKeyUsageDailyEntity, UUID> {

    /**
     * Upsert de l'agrégat du jour. Les compteurs d'une journée ne pouvant que croître, on garde le
     * <b>maximum</b> entre la valeur stockée et celle du flush ({@code GREATEST}) : un flush ne peut
     * jamais faire régresser un historique.
     *
     * <p>C'est la seconde barrière contre la perte d'historique corrigée le 31/07/2026 (la première
     * étant, dans {@code ApiKeyUsageCompteur}, de ne plus confondre « compteur Redis expiré » et
     * « compteur à zéro »). Sans elle, n'importe quelle régression future du compteur remettrait
     * silencieusement les agrégats à zéro.
     */
    @Modifying
    @Query("""
            INSERT INTO api_key_usage_daily (id, api_key_id, jour, total, errors, p95_ms)
            VALUES (gen_random_uuid(), :apiKeyId, :jour, :total, :errors, :p95Ms)
            ON CONFLICT (api_key_id, jour)
            DO UPDATE SET total = GREATEST(api_key_usage_daily.total, :total),
                          errors = GREATEST(api_key_usage_daily.errors, :errors),
                          p95_ms = :p95Ms
            """)
    Mono<Long> upsert(UUID apiKeyId, LocalDate jour, long total, long errors, long p95Ms);

    Flux<ApiKeyUsageDailyEntity> findByApiKeyIdInAndJourGreaterThanEqual(Collection<UUID> apiKeyIds,
                                                                         LocalDate jour);
}
