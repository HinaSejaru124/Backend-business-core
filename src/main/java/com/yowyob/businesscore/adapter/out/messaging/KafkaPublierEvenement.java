package com.yowyob.businesscore.adapter.out.messaging;

import tools.jackson.databind.ObjectMapper;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Implémentation socle de la publication d'événements sur Kafka.
 * La charge est sérialisée en JSON et envoyée sur le topic {@code <prefix>.<type>}.
 *
 * <p><b>Résilience (best-effort).</b> {@link KafkaTemplate#send} peut bloquer le thread appelant
 * jusqu'à {@code max.block.ms} en attendant les métadonnées du broker. Sur cette pile réactive, le
 * thread appelant est une boucle d'événements Netty partagée par toutes les requêtes : un broker
 * injoignable y gèlerait tout le serveur. On protège donc l'envoi par trois garde-fous :
 * <ul>
 *   <li>exécution sur {@link Schedulers#boundedElastic()} — jamais sur la boucle d'événements ;</li>
 *   <li>{@link Mono#timeout(Duration)} pour plafonner l'attente ;</li>
 *   <li>swallow des erreurs : si Kafka est indisponible, l'événement est abandonné (log WARN) sans
 *       jamais bloquer ni faire échouer l'action métier appelante.</li>
 * </ul>
 * La publication d'événements est un effet de bord d'observabilité, pas une garantie transactionnelle.
 */
@Component
public class KafkaPublierEvenement implements PublierEvenement {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublierEvenement.class);

    /** Plafond d'attente d'un envoi ; complète {@code max.block.ms} côté producer (voir KafkaConfig). */
    private static final Duration ENVOI_TIMEOUT = Duration.ofSeconds(5);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;
    private final String topicPrefix;

    public KafkaPublierEvenement(KafkaTemplate<String, String> kafka,
                                 ObjectMapper objectMapper,
                                 @Value("${businesscore.events.topic-prefix:business-core}") String topicPrefix) {
        this.kafka = kafka;
        this.objectMapper = objectMapper;
        this.topicPrefix = topicPrefix;
    }

    @Override
    public Mono<Void> publier(String type, Object charge) {
        String topic = topicPrefix + "." + type;
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(charge))
                // Supplier différé : kafka.send(...) n'est invoqué qu'à la souscription, sur boundedElastic.
                .flatMap(json -> Mono.fromFuture(() -> kafka.send(topic, json)))
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(ENVOI_TIMEOUT)
                .onErrorResume(e -> {
                    log.warn("Publication Kafka ignorée (broker indisponible ?) topic={} : {}", topic, e.toString());
                    return Mono.empty();
                })
                .then();
    }
}
