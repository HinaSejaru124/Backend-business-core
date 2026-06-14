package com.yowyob.businesscore.adapter.out.kernel.clientapp;

import com.yowyob.businesscore.domain.port.out.KernelClientCredentials;
import com.yowyob.businesscore.domain.port.out.ProvisionnerAccesDev;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation socle de {@link ProvisionnerAccesDev}.
 * À l'inscription d'un développeur, crée une ClientApplication kernel dédiée (clientId + secret
 * générés par le Business Core) via POST /api/client-applications. Appelé hors contexte tenant
 * (l'inscription précède l'existence du tenant), donc sans JWT par tenant.
 */
@Component
public class ProvisionnerAccesDevAdapter implements ProvisionnerAccesDev {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebClient kernelWebClient;

    public ProvisionnerAccesDevAdapter(@Qualifier("kernelWebClient") WebClient kernelWebClient) {
        this.kernelWebClient = kernelWebClient;
    }

    @Override
    public Mono<KernelClientCredentials> provisionner(String planCode) {
        String clientId = "bc-" + UUID.randomUUID();
        String clientSecret = genererSecret();
        CreateClientApplicationRequest request = new CreateClientApplicationRequest(
                clientId,
                "Business Core — " + clientId,
                "ClientApplication provisionnée par le Business Core",
                clientSecret,
                planCode,
                List.of());

        return kernelWebClient.post()
                .uri("/api/client-applications")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new KernelClientCredentials(clientId, clientSecret));
    }

    @Override
    public Mono<KernelClientCredentials> roterSecret(String kernelClientId) {
        return kernelWebClient.post()
                .uri("/api/client-applications/{id}/rotate-secret", kernelClientId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new KernelClientCredentials(kernelClientId, extraireSecret(response)));
    }

    @SuppressWarnings("unchecked")
    private String extraireSecret(Map<?, ?> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.get("clientSecret") != null) {
            return dataMap.get("clientSecret").toString();
        }
        Object direct = response.get("clientSecret");
        return direct != null ? direct.toString() : "";
    }

    private String genererSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
