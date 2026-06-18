package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.domain.port.out.actor.RattacherAOrganisation;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/** POST /api/organizations/{orgId}/actors. */
@Component
public class RattacherAOrganisationKernelAdapter implements RattacherAOrganisation {

    private final KernelClient kernel;

    public RattacherAOrganisationKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record Ack(boolean ok) {}

    @Override
    public Mono<Void> rattacher(UUID organizationId, UUID acteurKernelId) {
        return kernel.postForOrganization(
                        organizationId,
                        "/api/organizations/" + organizationId + "/actors",
                        Map.of("actorId", acteurKernelId),
                        Ack.class)
                .then();
    }
}