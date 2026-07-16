package com.yowyob.businesscore.adapter.out.messaging;

import tools.jackson.databind.ObjectMapper;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Implémentation socle de la publication d'événements sur Kafka.
 * La charge est sérialisée en JSON et envoyée sur le topic {@code <prefix>.<type>}.
 */
@Component
public class KafkaPublierEvenement implements PublierEvenement {

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
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(charge))
                .flatMap(json -> Mono.fromFuture(kafka.send(topicPrefix + "." + type, json)))
                .then();
    }
}
