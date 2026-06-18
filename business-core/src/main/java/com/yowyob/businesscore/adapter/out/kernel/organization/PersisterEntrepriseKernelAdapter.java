package com.yowyob.businesscore.adapter.out.kernel.organization;

import com.yowyob.businesscore.domain.port.out.enterprise.PersisterEntreprise;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Component
public class PersisterEntrepriseKernelAdapter implements PersisterEntreprise {

    private final KernelClient kernel;

    public PersisterEntrepriseKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}

    @Override
    public Mono<UUID> creerOrganisationAvecAgence(String nomLocal) {
        // 1) POST /api/organizations -> orgId, 2) POST /api/organizations/{orgId}/agencies (agence par défaut).
        return kernel.post("/api/organizations", Map.of("name", nomLocal), KernelId.class)
                .map(KernelId::id)
                .flatMap(orgId -> kernel.postForOrganization(
                                orgId,
                                "/api/organizations/" + orgId + "/agencies",
                                Map.of("name", nomLocal + " — agence principale", "primary", true),
                                KernelId.class)
                        .thenReturn(orgId));
    }
}