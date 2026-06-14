package com.yowyob.businesscore.adapter.out.kernel;

import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Client kernel de base (socle). Résout le JWT du tenant courant (clé -> token, mis en cache) et
 * l'injecte automatiquement dans chaque appel. Les adapters des features l'utilisent sans gérer
 * l'authentification. Timeout et retry exponentiel appliqués (résilience légère via Reactor ;
 * resilience4j-reactor disponible pour ajouter un circuit breaker).
 */
@Component
public class KernelClient {

    private final WebClient kernelWebClient;
    private final KernelTokenService tokenService;
    private final KernelCredentialStore credentialStore;
    private final Duration timeout;
    private final int maxRetries;

    public KernelClient(@Qualifier("kernelWebClient") WebClient kernelWebClient,
                        KernelTokenService tokenService,
                        KernelCredentialStore credentialStore,
                        KernelProperties properties) {
        this.kernelWebClient = kernelWebClient;
        this.tokenService = tokenService;
        this.credentialStore = credentialStore;
        this.timeout = Duration.ofMillis(properties.timeoutMs());
        this.maxRetries = properties.maxRetries();
    }

    public <T> Mono<T> get(String path, Class<T> type) {
        return jwtCourant().flatMap(jwt -> kernelWebClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .retrieve()
                .bodyToMono(type)
                .transform(this::resilience));
    }

    public <T> Mono<T> post(String path, Object body, Class<T> type) {
        return jwtCourant().flatMap(jwt -> kernelWebClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(type)
                .transform(this::resilience));
    }

    private Mono<String> jwtCourant() {
        return credentialStore.pourTenantCourant()
                .flatMap(creds -> tokenService.tokenPour(creds.clientId(), creds.secret()));
    }

    private <T> Mono<T> resilience(Mono<T> source) {
        Mono<T> avecTimeout = source.timeout(timeout);
        return maxRetries > 0
                ? avecTimeout.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(200)))
                : avecTimeout;
    }
}
