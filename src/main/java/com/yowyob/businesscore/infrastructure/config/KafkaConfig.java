package com.yowyob.businesscore.infrastructure.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka explicite (socle).
 *
 * <p>En Spring Boot 4 modularisé, l'auto-configuration Kafka n'est pas garantie par la seule présence
 * de {@code spring-kafka}. Le socle fournit donc lui-même le {@link ProducerFactory} et le
 * {@link KafkaTemplate}{@code <String, String>} utilisés par {@code PublierEvenement}. Le producer est
 * paresseux : aucune connexion au broker n'est tentée au démarrage.
 *
 * <p>Sécurité : PLAINTEXT en local. En prod sur le réseau yowyob, le broker exige
 * <b>SASL_PLAINTEXT / SCRAM-SHA-256</b> ; les propriétés {@code spring.kafka.security.protocol},
 * {@code spring.kafka.properties.sasl.mechanism} et {@code spring.kafka.properties.sasl.jaas.config}
 * (fournies par variables d'env {@code SPRING_KAFKA_*}) sont alors appliquées si présentes.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.security.protocol:}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Sécurité SASL appliquée uniquement si configurée (prod yowyob).
        if (!securityProtocol.isBlank()) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        }
        if (!saslMechanism.isBlank()) {
            props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        }
        if (!saslJaasConfig.isBlank()) {
            props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        }
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
