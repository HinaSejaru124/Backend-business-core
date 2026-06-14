package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Port de sortie — lance un workflow sur le moteur Saga du kernel (compensation gratuite).
 * Le moteur Saga est interne au kernel ; l'adapter orchestre via ses mécanismes.
 */
public interface ExecuterWorkflow {

    Mono<String> demarrer(String workflowNom, Map<String, Object> entrees);

    Mono<Void> compenser(String workflowId);
}
