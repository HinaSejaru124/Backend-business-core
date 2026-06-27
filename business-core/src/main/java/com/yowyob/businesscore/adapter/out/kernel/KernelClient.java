package com.yowyob.businesscore.adapter.out.kernel;

import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.security.KernelTokenHolder;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;
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
 *
 * <p><b>Enveloppe des réponses.</b> Le kernel enveloppe la plupart de ses réponses dans
 * {@code {success, data, message, errorCode, timestamp}} mais pas toutes (certaines renvoient l'objet
 * brut). Le client <b>détecte</b> l'enveloppe et n'expose que le {@code data} ; un {@code errorCode}
 * non nul (erreur métier même en HTTP 200) est remonté en erreur. Les réponses brutes passent
 * inchangées — un seul point central, les adapters n'ont rien à savoir de l'enveloppe.
 */
@Component
public class KernelClient {

    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    public static final String HEADER_API_KEY = "X-Api-Key";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_ORGANIZATION_ID = "X-Organization-Id";

    private final WebClient kernelWebClient;
    private final KernelTokenService tokenService;
    private final KernelCredentialStore credentialStore;
    private final ObjectMapper objectMapper;
    private final String appClientId;
    private final String appApiKey;
    private final Duration timeout;
    private final int maxRetries;

    public KernelClient(@Qualifier("kernelWebClient") WebClient kernelWebClient,
                        KernelTokenService tokenService,
                        KernelCredentialStore credentialStore,
                        ObjectMapper objectMapper,
                        KernelProperties properties) {
        this.kernelWebClient = kernelWebClient;
        this.tokenService = tokenService;
        this.credentialStore = credentialStore;
        this.objectMapper = objectMapper;
        this.appClientId = properties.clientId();
        this.appApiKey = properties.clientSecret();
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
        return KernelTokenHolder.currentToken().flatMap(tokenDelegue ->
                tokenDelegue.isPresent()
                        ? exchangeDelegue(method, path, body, type, organizationId, tokenDelegue.get())
                        : exchangeMachine(method, path, body, type, organizationId));
    }

    /**
     * Flux <b>délégué</b> (cible) : re-transmet le JWT de l'utilisateur courant en {@code Bearer}, avec
     * l'identité d'application BC ({@code X-Client-Id}/{@code X-Api-Key} du socle) et {@code X-Tenant-Id}.
     */
    private <T> Mono<T> exchangeDelegue(HttpMethod method, String path, Object body, Class<T> type,
                                        UUID organizationId, String tokenUtilisateur) {
        return BusinessContextHolder.currentTenantId().flatMap(tenant ->
                envoyer(method, path, body, type, appClientId, appApiKey, tokenUtilisateur,
                        tenant.map(UUID::toString).orElse(null), organizationId));
    }

    /**
     * Flux <b>machine</b> (client-credentials) : repli quand aucun token délégué n'est présent dans le
     * contexte (appels système ou routes non encore migrées). Comportement historique inchangé.
     */
    private <T> Mono<T> exchangeMachine(HttpMethod method, String path, Object body, Class<T> type,
                                        UUID organizationId) {
        return credentialStore.pourTenantCourant().flatMap(creds ->
                tokenService.tokenPour(creds.clientId(), creds.secret()).flatMap(jwt ->
                        envoyer(method, path, body, type, creds.clientId(), creds.secret(), jwt,
                                null, organizationId)));
    }

    private <T> Mono<T> envoyer(HttpMethod method, String path, Object body, Class<T> type,
                                String clientId, String apiKey, String bearer,
                                String tenantId, UUID organizationId) {
        WebClient.RequestBodySpec spec = kernelWebClient.method(method).uri(path);
        spec.header(HEADER_CLIENT_ID, clientId)
                .header(HEADER_API_KEY, apiKey)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        if (tenantId != null) {
            spec.header(HEADER_TENANT_ID, tenantId);
        }
        if (organizationId != null) {
            spec.header(HEADER_ORGANIZATION_ID, organizationId.toString());
        }
        WebClient.RequestHeadersSpec<?> finalSpec = (body != null) ? spec.bodyValue(body) : spec;
        return finalSpec.retrieve()
                .bodyToMono(Map.class)
                .transform(this::resilience)
                .flatMap(corps -> extraireData(corps, type));
    }

    /**
     * Désenveloppe la réponse : si le corps a la forme {@code {success, data, ...}}, n'expose que
     * {@code data} (et remonte un {@code errorCode} non nul en erreur) ; sinon renvoie le corps brut.
     */
    private <T> Mono<T> extraireData(Map<?, ?> corps, Class<T> type) {
        Object charge = corps;
        if (corps.containsKey("success") && corps.containsKey("data")) {
            Object errorCode = corps.get("errorCode");
            if (errorCode != null) {
                return Mono.error(new KernelException(errorCode.toString(),
                        String.valueOf(corps.get("message"))));
            }
            charge = corps.get("data");
        }
        if (type == Void.class || charge == null) {
            return Mono.empty();
        }
        return Mono.just(objectMapper.convertValue(charge, type));
    }

    private <T> Mono<T> resilience(Mono<T> source) {
        Mono<T> avecTimeout = source.timeout(timeout);
        return maxRetries > 0
                ? avecTimeout.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(200)))
                : avecTimeout;
    }
}
