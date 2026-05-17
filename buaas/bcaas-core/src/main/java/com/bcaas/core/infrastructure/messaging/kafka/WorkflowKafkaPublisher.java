package com.bcaas.core.infrastructure.messaging.kafka;

import com.bcaas.core.workflow.domain.event.WorkflowDomainEvent;
import com.bcaas.core.workflow.port.output.WorkflowEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class WorkflowKafkaPublisher implements WorkflowEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventSerializer serializer;
    private final String topic;

    public WorkflowKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventSerializer serializer,
            @Value("${bcaas.kafka.topics.workflow-events:bcaas.workflow.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;
        this.topic = topic;
    }

    @Override
    public Mono<Void> publish(WorkflowDomainEvent event) {
        return Mono.fromCallable(() -> {
            String payload = serializer.serialize(event);
            kafkaTemplate.send(topic, event.tenantId().toString(), payload);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
