package com.bcaas.core.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration Kafka — Topics BCaaS.
 * Couche 2 (Transport & Messaging) de la pile protocolaire BCaaS.
 * Analogie réseau : définition des canaux de communication.
 */
@Configuration
public class KafkaConfig {

    @Value("${bcaas.kafka.topics.tenant-events:bcaas.tenant.events}")
    private String tenantEventsTopic;

    @Value("${bcaas.kafka.topics.actor-events:bcaas.actor.events}")
    private String actorEventsTopic;

    @Value("${bcaas.kafka.topics.resource-events:bcaas.resource.events}")
    private String resourceEventsTopic;

    @Value("${bcaas.kafka.topics.workflow-events:bcaas.workflow.events}")
    private String workflowEventsTopic;

    @Value("${bcaas.kafka.topics.audit-events:bcaas.audit.events}")
    private String auditEventsTopic;

    @Bean
    public NewTopic tenantEventsTopic() {
        return TopicBuilder.name(tenantEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic actorEventsTopic() {
        return TopicBuilder.name(actorEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic resourceEventsTopic() {
        return TopicBuilder.name(resourceEventsTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name(workflowEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(auditEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
