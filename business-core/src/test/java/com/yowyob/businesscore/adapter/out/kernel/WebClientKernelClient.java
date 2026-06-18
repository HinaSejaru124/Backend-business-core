package com.yowyob.businesscore.adapter.out.kernel;

import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implémentation HTTP minimale de KernelClient, UNIQUEMENT pour les tests WireMock.
 * Le vrai socle fournit son propre KernelClient (auth + résilience) ; on reproduit juste
 * le contrat HTTP (méthode, chemin, en-tête X-Organization-Id) pour valider nos adapters.
 */
class WebClientKernelClient implements KernelClient {

    private final WebClient webClient;

    WebClientKernelClient(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public <T> Mono<T> post(String path, Object body, Class<T> type) {
        return webClient.post().uri(path).bodyValue(body).retrieve().bodyToMono(type);
    }

    @Override
    public <T> Mono<T> get(String path, Class<T> type) {
        return webClient.get().uri(path).retrieve().bodyToMono(type);
    }

    @Override
    public <T> Mono<T> postForOrganization(UUID organizationId, String path, Object body, Class<T> type) {
        return webClient.post().uri(path)
                .header("X-Organization-Id", organizationId.toString())
                .bodyValue(body).retrieve().bodyToMono(type);
    }

    @Override
    public <T> Mono<T> getForOrganization(UUID organizationId, String path, Class<T> type) {
        return webClient.get().uri(path)
                .header("X-Organization-Id", organizationId.toString())
                .retrieve().bodyToMono(type);
    }
}
