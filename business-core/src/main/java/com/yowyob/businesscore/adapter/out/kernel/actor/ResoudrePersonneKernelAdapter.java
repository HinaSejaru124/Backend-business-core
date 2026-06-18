package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.ResoudrePersonne;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/** Opérateur (interne) -> actor-core kernel : POST /api/actors. */
@Component
public class ResoudrePersonneKernelAdapter implements ResoudrePersonne {

    private final KernelClient kernel;

    public ResoudrePersonneKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<UUID> resoudreOperateur(String identifiant, String nom) {
        return kernel.post("/api/actors", Map.of("identifier", identifiant, "name", nom), KernelId.class)
                .map(KernelId::id);
    }
}
