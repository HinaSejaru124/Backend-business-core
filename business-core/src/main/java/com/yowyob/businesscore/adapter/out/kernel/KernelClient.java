package com.yowyob.businesscore.adapter.out.kernel;

import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

/**
 * Client kernel de base (socle). Applique le modèle d'authentification réel du kernel RT-Comops :
 *
 * <ul>
 *   <li><b>{@code X-Client-Id} / {@code X-Api-Key}</b> sur <b>chaque</b> appel {@code /api/**} :
 *       l'identité de la ClientApplication du développeur courant (résolue depuis le tenant du
 *       BusinessContext).</li>
 *   <li><b>{@code Authorization: Bearer}</b> : JWT court obtenu par échange de la clé kernel
 *       ({@code /oauth2/token}) et mis en cache Redis ; requis par les endpoints protégés.</li>
 *   <li><b>{@code X-Organization-Id}</b> : pour les opérations liées à une organisation (entreprise),
 *       via les variantes {@code *ForOrganization}.</li>
 * </ul>
 *
 * Les adapters des features l'utilisent sans gérer l'authentification. Timeout + retry exponentiel
 * (résilience légère).
 */
@Component
public class KernelClient {

    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_API_KEY = "X-Api-Key";
    public static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";

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
        return exchange(HttpMethod.GET, path, null, type, null);
    }

    public <T> Mono<T> post(String path, Object body, Class<T> type) {
        return exchange(HttpMethod.POST, path, body, type, null);
    }

    /** Variante pour une opération liée à une organisation (ajoute {@code X-Organization-Id}). */
    public <T> Mono<T> getForOrganization(String path, Class<T> type, UUID organizationId) {
        return exchange(HttpMethod.GET, path, null, type, organizationId);
    }

    /** Variante pour une opération liée à une organisation (ajoute {@code X-Organization-Id}). */
    public <T> Mono<T> postForOrganization(String path, Object body, Class<T> type, UUID organizationId) {
        return exchange(HttpMethod.POST, path, body, type, organizationId);
    }

    private <T> Mono<T> exchange(HttpMethod method, String path, Object body, Class<T> type, UUID organizationId) {
        return credentialStore.pourTenantCourant().flatMap(creds ->
                tokenService.tokenPour(creds.clientId(), creds.secret()).flatMap(jwt -> {
                    WebClient.RequestBodySpec spec = kernelWebClient.method(method).uri(path);
                    spec.header(HEADER_CLIENT_ID, creds.clientId())
                            .header(HEADER_API_KEY, creds.secret())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
                    if (organizationId != null) {
                        spec.header(HEADER_ORGANIZATION_ID, organizationId.toString());
                    }
                    WebClient.RequestHeadersSpec<?> finalSpec = (body != null) ? spec.bodyValue(body) : spec;
                    return finalSpec.retrieve().bodyToMono(type).transform(this::resilience);
                }));
    }

    private <T> Mono<T> resilience(Mono<T> source) {
        Mono<T> avecTimeout = source.timeout(timeout);
        return maxRetries > 0
                ? avecTimeout.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(200)))
                : avecTimeout;
    }
}
