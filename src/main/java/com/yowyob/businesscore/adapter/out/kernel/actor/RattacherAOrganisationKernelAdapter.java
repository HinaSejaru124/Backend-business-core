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

    @Override
    public Mono<Void> rattacher(UUID organizationId, UUID actorId) {
        // kernel.post() sans X-Organization-Id : le kernel renvoie 401 si l'en-tête est présent EN PLUS
        // de l'id déjà dans l'URL (même bug déjà constaté et corrigé sur /agencies, /services, /approve
        // — cf. PersisterEntrepriseKernelAdapter). Vérifié empiriquement le 13/07/2026 : 401 avec
        // l'en-tête, 201 sans, sur ce même endpoint /actors.
        //
        // Réponse typée en Map (pas un record strict) : le kernel renvoie {id, organizationId, actorId,
        // type} — jamais {ok: boolean} comme le supposait l'ancien record Ack, qui faisait donc toujours
        // échouer la désérialisation (MismatchedInputException) une fois l'appel enfin authentifié
        // correctement. On ne se sert pas du contenu ici (.then() l'ignore), donc Map suffit et reste
        // robuste si le kernel fait évoluer sa forme de réponse.
        return kernel.post(
                        "/api/organizations/" + organizationId + "/actors",
                        Map.of("actorId", actorId),
                        Map.class)
                .then();
    }
}
