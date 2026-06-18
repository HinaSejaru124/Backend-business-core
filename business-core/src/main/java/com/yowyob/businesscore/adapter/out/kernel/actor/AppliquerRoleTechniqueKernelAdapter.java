package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.AppliquerRoleTechnique;
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
    public Mono<Void> appliquer(UUID actorId, String roleCode) {
        return kernel.post("/api/roles", Map.of("code", roleCode), KernelId.class)
                .flatMap(role -> kernel.post(
                        "/api/roles/assignments",
                        Map.of("roleId", role.id(), "actorId", actorId),
                        KernelId.class))
                .then();
    }
}
