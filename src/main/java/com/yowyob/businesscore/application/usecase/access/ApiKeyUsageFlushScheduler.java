package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Draine périodiquement les compteurs d'usage Redis vers la base ({@code api_key_usage_daily}).
 * L'upsert écrit une valeur absolue par (clé, jour) : le flush est idempotent (pas de double comptage).
 * Le dashboard lit ensuite ces agrégats pour l'historique.
 */
@Component
public class ApiKeyUsageFlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyUsageFlushScheduler.class);

    private final ApiKeyUsageCompteur compteur;
    private final ApiKeyUsageDailyRepository repository;

    public ApiKeyUsageFlushScheduler(ApiKeyUsageCompteur compteur,
                                     ApiKeyUsageDailyRepository repository) {
        this.compteur = compteur;
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${businesscore.api-keys.usage-flush-ms:60000}",
            initialDelayString = "${businesscore.api-keys.usage-flush-ms:60000}")
    public void flush() {
        compteur.drainPourFlush()
                .flatMap(u -> repository.upsert(u.apiKeyId(), u.jour(), u.total(), u.errors(), 0L))
                .onErrorContinue((ex, o) -> log.warn("Flush usage clé API échoué pour {} : {}", o, ex.toString()))
                .subscribe();
    }
}
