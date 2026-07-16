package com.yowyob.businesscore.adapter.out.cache;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Compteur mensuel de requêtes par développeur (Redis, O(1)) — source de l'enforcement des quotas.
 *
 * <p>Une valeur absolue par (développeur, mois), TTL ~40 j. En cas d'absence (éviction / démarrage /
 * nouveau mois), {@code QuotaService} la reconstruit depuis la base ({@code api_key_usage_daily} +
 * compteurs live du jour) puis {@link #initialiser} la pose ; elle est ensuite maintenue par
 * {@link #incrementer}. Le comptage ne doit jamais faire échouer une requête (best-effort).
 */
@Component
public class QuotaMensuelCompteur {

    private static final String PREFIX = "bc:quota:";
    private static final Duration TTL = Duration.ofDays(40);

    private final ReactiveStringRedisTemplate redis;

    public QuotaMensuelCompteur(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Valeur courante, ou {@link Mono#empty()} si le compteur n'existe pas encore. */
    public Mono<Long> lire(UUID developerId, YearMonth mois) {
        return redis.opsForValue().get(cle(developerId, mois)).map(Long::parseLong);
    }

    /** Pose la valeur de base seulement si le compteur est absent (idempotent, sans écraser). */
    public Mono<Boolean> initialiser(UUID developerId, YearMonth mois, long base) {
        return redis.opsForValue().setIfAbsent(cle(developerId, mois), Long.toString(base), TTL)
                .onErrorReturn(Boolean.FALSE);
    }

    /** Incrémente d'une unité et prolonge le TTL (best-effort). */
    public Mono<Long> incrementer(UUID developerId, YearMonth mois) {
        String k = cle(developerId, mois);
        return redis.opsForValue().increment(k)
                .flatMap(v -> redis.expire(k, TTL).thenReturn(v))
                .onErrorResume(ex -> Mono.empty());
    }

    private static String cle(UUID developerId, YearMonth mois) {
        return PREFIX + developerId + ":" + mois; // YearMonth.toString() => "2026-07"
    }
}
