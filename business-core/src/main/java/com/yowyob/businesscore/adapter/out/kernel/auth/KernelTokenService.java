package com.yowyob.businesscore.adapter.out.kernel.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Échange une clé kernel (clientId + secret) contre un JWT court via {@code POST /oauth2/token}
 * (grant client_credentials, en-tête Basic), et met le token en cache (TTL = expiration - marge).
 */
@Service
public class KernelTokenService {

    private static final Duration MARGE = Duration.ofSeconds(30);

    private final WebClient kernelWebClient;
    private final JwtCache cache;

    public KernelTokenService(@Qualifier("kernelWebClient") WebClient kernelWebClient, JwtCache cache) {
        this.kernelWebClient = kernelWebClient;
        this.cache = cache;
    }

    public Mono<String> tokenPour(String clientId, String secret) {
        return cache.get(clientId)
                .switchIfEmpty(Mono.defer(() -> demanderEtCacher(clientId, secret)));
    }

    private Mono<String> demanderEtCacher(String clientId, String secret) {
        String basic = Base64.getEncoder()
                .encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
        return kernelWebClient.post()
                .uri("/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("grant_type=client_credentials")
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(reponse -> extraireEtCacher(clientId, reponse));
    }

    private Mono<String> extraireEtCacher(String clientId, Map<?, ?> reponse) {
        Object accessToken = reponse.get("access_token");
        if (accessToken == null) {
            return Mono.error(new IllegalStateException("Réponse /oauth2/token sans access_token"));
        }
        long expiresIn = reponse.get("expires_in") instanceof Number n ? n.longValue() : 300L;
        Duration ttl = Duration.ofSeconds(Math.max(1, expiresIn - MARGE.toSeconds()));
        String token = accessToken.toString();
        return cache.put(clientId, token, ttl).thenReturn(token);
    }
}
