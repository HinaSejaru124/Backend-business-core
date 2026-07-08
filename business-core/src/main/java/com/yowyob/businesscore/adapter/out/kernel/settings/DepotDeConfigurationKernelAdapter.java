package com.yowyob.businesscore.adapter.out.kernel.settings;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.DepotDeConfiguration;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;

/**
 * Adapter kernel — lit/écrit la politique opérationnelle d'une organisation
 * ({@code GET/PUT /api/settings/organizations/{orgId}/operational-policy}).
 */
@Component
public class DepotDeConfigurationKernelAdapter implements DepotDeConfiguration {

    private final KernelClient kernel;
    private final ObjectMapper objectMapper;

    public DepotDeConfigurationKernelAdapter(KernelClient kernel, ObjectMapper objectMapper) {
        this.kernel = kernel;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> lire(UUID organizationId, String cle) {
        return kernel.getForOrganization(
                        "/api/settings/organizations/" + organizationId + "/operational-policy",
                        JsonNode.class,
                        organizationId)
                .defaultIfEmpty(objectMapper.createObjectNode())
                .map(policy -> {
                    JsonNode valeur = policy.get(cle);
                    return valeur == null || valeur.isNull() ? "" : valeur.asString();
                });
    }

    @Override
    public Mono<Void> ecrire(UUID organizationId, String cle, String valeur) {
        return kernel.getForOrganization(
                        "/api/settings/organizations/" + organizationId + "/operational-policy",
                        JsonNode.class,
                        organizationId)
                .defaultIfEmpty(objectMapper.createObjectNode())
                .flatMap(policy -> {
                    ObjectNode miseAJour = policy.isObject()
                            ? (ObjectNode) policy.deepCopy()
                            : objectMapper.createObjectNode();
                    if (valeur == null || valeur.isBlank()) {
                        miseAJour.remove(cle);
                    } else if ("true".equalsIgnoreCase(valeur) || "false".equalsIgnoreCase(valeur)) {
                        miseAJour.put(cle, Boolean.parseBoolean(valeur));
                    } else {
                        try {
                            miseAJour.put(cle, Integer.parseInt(valeur));
                        } catch (NumberFormatException ex) {
                            miseAJour.put(cle, valeur);
                        }
                    }
                    return kernel.putForOrganization(
                            "/api/settings/organizations/" + organizationId + "/operational-policy",
                            miseAJour,
                            Void.class,
                            organizationId);
                })
                .then();
    }
}
