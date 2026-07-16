package com.yowyob.businesscore.adapter.out.cache;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compteurs d'usage des clés API sur Redis (INCR non bloquant, par clé et par jour). Alimenté par le
 * filtre d'usage à chaque requête authentifiée ; drainé périodiquement vers la base (dashboard).
 *
 * <p>Les compteurs portent une valeur absolue pour le jour courant (monotone), avec un TTL de quelques
 * jours pour l'auto-nettoyage. Le flush réécrit cette valeur absolue en base (idempotent).
 */
@Component
public class ApiKeyUsageCompteur {

    private static final String PREFIX = "bc:apiusage:";
    private static final String PENDING = PREFIX + "pending";
    private static final Duration TTL = Duration.ofDays(3);

    private final ReactiveStringRedisTemplate redis;

    public ApiKeyUsageCompteur(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Instantané d'un compteur journalier (pour le flush vers la base). */
    public record UsageJournalier(UUID apiKeyId, LocalDate jour, long total, long errors) {
    }

    /** Enregistre une requête (et une erreur le cas échéant) pour la clé, sur le jour courant. */
    public Mono<Void> enregistrer(UUID apiKeyId, boolean erreur) {
        LocalDate jour = LocalDate.now();
        String membre = apiKeyId + "|" + jour;
        String cleTotal = totalKey(apiKeyId, jour);
        String cleErreurs = errorsKey(apiKeyId, jour);

        Mono<Void> total = redis.opsForValue().increment(cleTotal)
                .flatMap(v -> redis.expire(cleTotal, TTL)).then();
        Mono<Void> erreurs = erreur
                ? redis.opsForValue().increment(cleErreurs)
                        .flatMap(v -> redis.expire(cleErreurs, TTL)).then()
                : Mono.empty();
        Mono<Void> pending = redis.opsForSet().add(PENDING, membre).then();

        return total.then(erreurs).then(pending)
                .onErrorResume(ex -> Mono.empty()); // l'usage ne doit jamais casser une requête
    }

    /** Draine les compteurs en attente sous forme d'instantanés pour le flush en base. */
    public Flux<UsageJournalier> drainPourFlush() {
        return redis.opsForSet().members(PENDING)
                .flatMap(this::lire);
    }

    /** Lit les compteurs live d'une clé pour un jour donné (source de vérité du jour courant). */
    public Mono<UsageJournalier> lireJour(UUID apiKeyId, LocalDate jour) {
        Mono<Long> total = redis.opsForValue().get(totalKey(apiKeyId, jour))
                .map(Long::parseLong).defaultIfEmpty(0L);
        Mono<Long> errors = redis.opsForValue().get(errorsKey(apiKeyId, jour))
                .map(Long::parseLong).defaultIfEmpty(0L);
        return Mono.zip(total, errors)
                .map(t -> new UsageJournalier(apiKeyId, jour, t.getT1(), t.getT2()));
    }

    private Mono<UsageJournalier> lire(String membre) {
        int sep = membre.lastIndexOf('|');
        if (sep < 0) {
            return Mono.empty();
        }
        UUID apiKeyId;
        LocalDate jour;
        try {
            apiKeyId = UUID.fromString(membre.substring(0, sep));
            jour = LocalDate.parse(membre.substring(sep + 1));
        } catch (RuntimeException ex) {
            return redis.opsForSet().remove(PENDING, membre).then(Mono.empty());
        }
        Mono<Long> total = redis.opsForValue().get(totalKey(apiKeyId, jour)).map(Long::parseLong).defaultIfEmpty(0L);
        Mono<Long> errors = redis.opsForValue().get(errorsKey(apiKeyId, jour)).map(Long::parseLong).defaultIfEmpty(0L);
        return Mono.zip(total, errors)
                .map(t -> new UsageJournalier(apiKeyId, jour, t.getT1(), t.getT2()));
    }

    private static String totalKey(UUID apiKeyId, LocalDate jour) {
        return PREFIX + apiKeyId + ":" + jour + ":t";
    }

    private static String errorsKey(UUID apiKeyId, LocalDate jour) {
        return PREFIX + apiKeyId + ":" + jour + ":e";
    }
}
