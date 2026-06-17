package com.yowyob.businesscore.adapter.out.kernel.workflow;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.ExecuterWorkflow;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Adapter kernel — implémente {@link ExecuterWorkflow} au-dessus du moteur Saga du kernel.
 * {@code demarrer} lance un workflow ({@code POST /api/sagas}) ; {@code compenser} déclenche
 * l'annulation des effets déjà produits ({@code POST /api/sagas/{id}/compensate}). Tout passe par
 * {@link KernelClient}.
 */
@Component
public class ExecuterWorkflowKernelAdapter implements ExecuterWorkflow {

    private final KernelClient kernel;

    public ExecuterWorkflowKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Mono<String> demarrer(String workflowNom, Map<String, Object> entrees) {
        DemarrerSagaRequest requete = new DemarrerSagaRequest(workflowNom, entrees);
        return kernel.post("/api/sagas", requete, SagaResponse.class)
                .map(SagaResponse::id);
    }

    @Override
    public Mono<Void> compenser(String workflowId) {
        return kernel.post("/api/sagas/" + workflowId + "/compensate", null, Void.class);
    }
}

record DemarrerSagaRequest(String name, Map<String, Object> inputs) {
}

record SagaResponse(String id) {
}
