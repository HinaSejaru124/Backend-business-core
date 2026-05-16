package com.bcaas.core.workflow.port.output;

import com.bcaas.core.workflow.domain.event.WorkflowDomainEvent;
import reactor.core.publisher.Mono;

public interface WorkflowEventPublisher {
    Mono<Void> publish(WorkflowDomainEvent event);
}
