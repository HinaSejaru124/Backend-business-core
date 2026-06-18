package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.domain.port.out.actor.AppliquerRoleTechnique;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/** POST /api/roles PUIS POST /api/roles/assignments (2 appels enchaînés). */
@Component
public class AppliquerRoleTechniqueKernelAdapter implements AppliquerRoleTechnique {

    private final KernelClient kernel;

    public AppliquerRoleTechniqueKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<Void> appliquer(String codeRole, UUID actorKernelId) {
        return kernel.post("/api/roles", Map.of("code", codeRole), KernelId.class)
                .flatMap(role -> kernel.post(
                        "/api/roles/assignments",
                        Map.of("roleId", role.id(), "actorId", actorKernelId),
                        KernelId.class))
                .then();
    }
}