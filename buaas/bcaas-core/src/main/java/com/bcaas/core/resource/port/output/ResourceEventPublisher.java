package com.bcaas.core.resource.port.output;

import com.bcaas.core.resource.domain.event.ResourceDomainEvent;
import reactor.core.publisher.Mono;

public interface ResourceEventPublisher {
    Mono<Void> publish(ResourceDomainEvent event);
}
