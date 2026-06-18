package com.yowyob.businesscore.adapter.out.kernel.actor;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.RattacherAOrganisation;
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
    public Mono<Void> rattacher(UUID organizationId, UUID actorId) {
        return kernel.postForOrganization(
                        "/api/organizations/" + organizationId + "/actors",
                        Map.of("actorId", actorId),
                        Ack.class,
                        organizationId)
                .then();
    }
}
