package com.yowyob.businesscore.adapter.out.kernel.organization;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.PersisterEntreprise;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Crée l'organisation réelle d'une entreprise dans le kernel.
 * {@code POST /api/organizations} puis {@code POST /api/organizations/{orgId}/agencies}.
 */
@Component
public class PersisterEntrepriseKernelAdapter implements PersisterEntreprise {

    private final KernelClient kernel;

    public PersisterEntrepriseKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<UUID> creerOrganisation(String nom) {
        return kernel.post("/api/organizations", Map.of("name", nom), KernelId.class)
                .map(KernelId::id);
    }

    @Override
    public Mono<UUID> creerAgence(UUID organizationId, String nom) {
        return kernel.postForOrganization(
                        "/api/organizations/" + organizationId + "/agencies",
                        Map.of("name", nom, "primary", true),
                        KernelId.class,
                        organizationId)
                .map(KernelId::id);
    }
}
