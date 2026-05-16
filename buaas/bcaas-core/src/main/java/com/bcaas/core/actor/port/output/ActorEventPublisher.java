package com.bcaas.core.actor.port.output;

import com.bcaas.core.actor.domain.event.ActorDomainEvent;
import reactor.core.publisher.Mono;

public interface ActorEventPublisher {
    Mono<Void> publish(ActorDomainEvent event);
}
