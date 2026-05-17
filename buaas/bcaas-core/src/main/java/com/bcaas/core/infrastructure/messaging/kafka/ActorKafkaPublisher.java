package com.bcaas.core.infrastructure.messaging.kafka;

import com.bcaas.core.actor.domain.event.ActorDomainEvent;
import com.bcaas.core.actor.port.output.ActorEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class ActorKafkaPublisher implements ActorEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventSerializer serializer;
    private final String topic;

    public ActorKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventSerializer serializer,
            @Value("${bcaas.kafka.topics.actor-events:bcaas.actor.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(ActorDomainEvent event) {
        return Mono.fromCallable(() -> {
            String payload = serializer.serialize(event);
            kafkaTemplate.send(topic, event.tenantId().toString(), payload);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
