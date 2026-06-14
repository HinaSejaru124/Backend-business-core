package com.yowyob.businesscore.adapter.out.kernel.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Test témoin de l'authentification kernel (kernel mické par WireMock).
 * Vérifie que le service obtient un JWT via /oauth2/token et le met en cache (le 2e appel ne
 * recontacte pas le kernel).
 */
class KernelTokenServiceTest {

    private WireMockServer wireMock;
    private KernelTokenService tokenService;

    /** Cache JWT en mémoire pour le test (équivalent fonctionnel de la version Redis). */
    private static final class CacheMemoire implements JwtCache {
        private final Map<String, String> map = new ConcurrentHashMap<>();

        @Override
        public Mono<String> get(String key) {
            return Mono.justOrEmpty(map.get(key));
        }

        @Override
        public Mono<Void> put(String key, String token, Duration ttl) {
            map.put(key, token);
            return Mono.empty();
        }
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"access_token\":\"jwt-court-123\",\"token_type\":\"Bearer\",\"expires_in\":3600}")));

        WebClient client = WebClient.builder().baseUrl("http://localhost:" + wireMock.port()).build();
        tokenService = new KernelTokenService(client, new CacheMemoire());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void obtient_un_jwt_puis_sert_depuis_le_cache() {
        StepVerifier.create(tokenService.tokenPour("client-1", "secret-1"))
                .expectNext("jwt-court-123")
                .verifyComplete();

        // Deuxième appel : servi par le cache, aucun nouvel appel kernel.
        StepVerifier.create(tokenService.tokenPour("client-1", "secret-1"))
                .expectNext("jwt-court-123")
                .verifyComplete();

        wireMock.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")));
    }
}
