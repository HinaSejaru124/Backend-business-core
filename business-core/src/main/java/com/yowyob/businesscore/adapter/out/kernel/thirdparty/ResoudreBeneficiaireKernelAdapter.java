package com.yowyob.businesscore.adapter.out.kernel.thirdparty;

import com.yowyob.businesscore.domain.port.out.actor.ResoudreBeneficiaire;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/** Bénéficiaire (externe) -> tiers kernel : POST /api/third-parties. Core DIFFÉRENT (RG-04). */
@Component
public class ResoudreBeneficiaireKernelAdapter implements ResoudreBeneficiaire {

    private final KernelClient kernel;

    public ResoudreBeneficiaireKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<UUID> resoudreBeneficiaire(String identifiant) {
        return kernel.post("/api/third-parties", Map.of("identifier", identifiant), KernelId.class)
                .map(KernelId::id);
    }
}