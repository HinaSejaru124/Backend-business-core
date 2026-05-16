package com.bcaas.core.tenant.port.output;

import com.bcaas.core.tenant.domain.event.TenantDomainEvent;
import reactor.core.publisher.Mono;

/**
 * Port de sortie — publication des événements domaine Tenant.
 * Derrière ce port peut se trouver Kafka, RabbitMQ ou tout autre broker.
 */
public interface TenantEventPublisher {

    Mono<Void> publish(TenantDomainEvent event);
}
