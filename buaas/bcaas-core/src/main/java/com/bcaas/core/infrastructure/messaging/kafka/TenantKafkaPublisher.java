package com.bcaas.core.infrastructure.messaging.kafka;

import com.bcaas.core.tenant.domain.event.TenantDomainEvent;
import com.bcaas.core.tenant.port.output.TenantEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Adapteur Kafka pour la publication des événements Tenant.
 * Implémente le port TenantEventPublisher défini par le domaine.
 *
 * Analogie réseau : couche transport — acheminement des paquets
 * vers le bon topic Kafka selon le type d'événement.
 */
@Component
public class TenantKafkaPublisher implements TenantEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventSerializer serializer;
    private final String topic;

    public TenantKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventSerializer serializer,
            @Value("${bcaas.kafka.topics.tenant-events:bcaas.tenant.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(TenantDomainEvent event) {
        return Mono.fromCallable(() -> {
            String payload = serializer.serialize(event);
            kafkaTemplate.send(topic, event.tenantId().toString(), payload);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
