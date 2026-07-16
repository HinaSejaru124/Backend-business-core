package com.pharmacore.pharmaciebackend.telemetry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Rapporte les requêtes <b>propres</b> de PharmaCore (catégorie APP) à Business Core, pour que le
 * développeur les voie toutes dans son track — Business Core ne pouvant pas les observer lui-même.
 *
 * <p>Best-effort : envoi asynchrone (pool dédié), toute erreur est avalée — la télémétrie ne doit
 * <b>jamais</b> ralentir ni casser une vraie requête. Utilise le client clé API existant
 * ({@code bcaasRestClient}) ; l'endpoint {@code /v1/telemetry/requests} est non facturable côté BC.
 */
@Component
public class TelemetryReporter {

    private static final Logger log = LoggerFactory.getLogger(TelemetryReporter.class);

    private final RestClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "telemetry-reporter");
        t.setDaemon(true);
        return t;
    });

    public TelemetryReporter(RestClient bcaasRestClient) {
        this.client = bcaasRestClient;
    }

    /** Rapporte une requête propre (fire-and-forget). */
    public void rapporter(String methode, String endpoint, int statutHttp, long dureeMs) {
        executor.submit(() -> {
            try {
                client.post()
                        .uri("/v1/telemetry/requests")
                        .body(Map.of("requetes", List.of(Map.of(
                                "methode", methode,
                                "endpoint", endpoint,
                                "statutHttp", statutHttp,
                                "dureeMs", dureeMs))))
                        .retrieve()
                        .toBodilessEntity();
            } catch (RuntimeException ex) {
                log.debug("Télémétrie non envoyée ({} {}) : {}", methode, endpoint, ex.toString());
            }
        });
    }
}
