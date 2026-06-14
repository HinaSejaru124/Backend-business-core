package com.yowyob.businesscore.adapter.out.kernel.clientapp;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.KernelClientCredentials;
import com.yowyob.businesscore.domain.port.out.ProvisionnerAccesDev;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
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
 *
 * <p>À l'inscription d'un développeur, crée une ClientApplication kernel dédiée via
 * {@code POST /api/client-applications}. Cet appel est fait <b>avant</b> que le développeur n'ait de
 * credentials kernel : il s'authentifie donc avec la <b>ClientApplication plateforme</b> du Business
 * Core ({@code KERNEL_CLIENT_ID}/{@code KERNEL_CLIENT_SECRET}) via {@code X-Client-Id}/{@code X-Api-Key}.
 * Il n'utilise pas le {@link KernelClient} (qui résout les credentials du développeur courant).
 */
@Component
public class ProvisionnerAccesDevAdapter implements ProvisionnerAccesDev {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebClient kernelWebClient;
    private final KernelProperties kernelProperties;

    public ProvisionnerAccesDevAdapter(@Qualifier("kernelWebClient") WebClient kernelWebClient,
                                       KernelProperties kernelProperties) {
        this.kernelWebClient = kernelWebClient;
        this.kernelProperties = kernelProperties;
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
                .headers(this::authentifierPlateforme)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .thenReturn(new KernelClientCredentials(clientId, clientSecret));
    }

    @Override
    public Mono<KernelClientCredentials> roterSecret(String kernelClientId) {
        return kernelWebClient.post()
                .uri("/api/client-applications/{id}/rotate-secret", kernelClientId)
                .headers(this::authentifierPlateforme)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> new KernelClientCredentials(kernelClientId, extraireSecret(response)));
    }

    /** Authentifie l'appel avec la ClientApplication plateforme du Business Core (si configurée). */
    private void authentifierPlateforme(org.springframework.http.HttpHeaders headers) {
        if (kernelProperties.aDesCredentialsPlateforme()) {
            headers.set(KernelClient.HEADER_CLIENT_ID, kernelProperties.clientId());
            headers.set(KernelClient.HEADER_API_KEY, kernelProperties.clientSecret());
        }
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
