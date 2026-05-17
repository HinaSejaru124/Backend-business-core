package com.bcaas.core.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

/**
 * Sérialiseur JSON des événements domaine pour Kafka.
 * Couche 2 (Transport & Messaging) de la pile protocolaire BCaaS.
 */
@Component
public class KafkaEventSerializer {

    private final ObjectMapper objectMapper;

    public KafkaEventSerializer() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Erreur sérialisation événement : " + e.getMessage(), e);
        }
    }
}
