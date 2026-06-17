package com.yowyob.businesscore.adapter.out.kernel.businessdomain;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.BusinessDomainRef;
import com.yowyob.businesscore.domain.port.out.ResoudreBusinessDomain;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Implémentation socle de {@link ResoudreBusinessDomain} : référence la taxonomie kernel des domaines
 * métier via {@code /api/business-domains}, authentifiée au nom du tenant courant par le
 * {@link KernelClient}.
 *
 * <p>Sollicitée uniquement lorsqu'un code de domaine est fourni à la création d'un Type Métier
 * (donc jamais au démarrage). Le mapping de la réponse kernel (enveloppe {@code data} ou objet direct)
 * est géré défensivement ; à confirmer contre le kernel réel.
 */
@Component
public class ResoudreBusinessDomainAdapter implements ResoudreBusinessDomain {

    private final KernelClient kernelClient;

    public ResoudreBusinessDomainAdapter(KernelClient kernelClient) {
        this.kernelClient = kernelClient;
    }

    @Override
    public Mono<UUID> resoudreOuCreer(String code, String nom) {
        Map<String, Object> body = Map.of("code", code, "nom", nom == null ? "" : nom);
        return kernelClient.post("/api/business-domains", body, Map.class)
                .map(reponse -> extraireId(reponse));
    }

    @Override
    public Flux<BusinessDomainRef> lister() {
        return kernelClient.get("/api/business-domains", BusinessDomainRef[].class)
                .flatMapMany(Flux::fromArray);
    }

    private UUID extraireId(Map<?, ?> reponse) {
        Object data = reponse.get("data");
        Object id = (data instanceof Map<?, ?> dataMap) ? dataMap.get("id") : reponse.get("id");
        if (id == null) {
            throw new IllegalStateException("Réponse kernel sans identifiant de business-domain");
        }
        return UUID.fromString(id.toString());
    }
}
