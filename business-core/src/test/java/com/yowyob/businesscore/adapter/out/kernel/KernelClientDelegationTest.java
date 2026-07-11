package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.in.security.JwtAuthenticationToken;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore.KernelCreds;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.security.KernelTokenHolder;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Vérifie le flux <b>délégué</b> de KernelClient : quand un token utilisateur est présent dans le Reactor
 * Context ({@link KernelTokenHolder}), l'appel kernel porte ce Bearer + {@code X-Tenant-Id} (du tenant
 * courant) + l'identité d'application BC ({@code X-Client-Id}/{@code X-Api-Key} de config).
 */
class KernelClientDelegationTest {

    private WireMockServer wireMock;
    private KernelClient kernel;
    private KernelTokenService tokenService;
    private KernelCredentialStore credentialStore;

    public record Echo(String value) {
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "bc-app", "bc-secret", "ORGANIZATION", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        tokenService = mock(KernelTokenService.class);
        credentialStore = mock(KernelCredentialStore.class);
        kernel = new KernelClient(
                webClient, tokenService, credentialStore,
                JsonMapper.builder().build(), props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("token délégué présent → Bearer utilisateur + X-Tenant-Id + identité app BC")
    void transmet_le_token_delegue() {
        UUID tenant = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/echo"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"value\":\"ok\"},\"errorCode\":null}")));

        BusinessContext ctx = new BusinessContext(tenant, null, Set.of(), null, "trace", Locale.FRENCH);

        StepVerifier.create(kernel.get("/api/echo", Echo.class)
                        .contextWrite(c -> KernelTokenHolder.withToken(
                                BusinessContextHolder.withContext(c, ctx), "user-token-xyz")))
                .assertNext(echo -> assertThat(echo.value()).isEqualTo("ok"))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/echo"))
                .withHeader("Authorization", equalTo("Bearer user-token-xyz"))
                .withHeader("X-Tenant-Id", equalTo(tenant.toString()))
                .withHeader("X-Client-Id", equalTo("bc-app"))
                .withHeader("X-Api-Key", equalTo("bc-secret")));
    }

    @Test
    @DisplayName("token délégué via SecurityContext (repli) → Bearer utilisateur")
    void transmet_le_token_depuis_security_context() {
        UUID tenant = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/echo"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"value\":\"ok\"},\"errorCode\":null}")));

        BusinessContext ctx = new BusinessContext(tenant, null, Set.of(), null, "trace", Locale.FRENCH);
        JwtAuthenticationToken auth = JwtAuthenticationToken.authenticated("jwt-from-security", ctx);
        SecurityContext securityContext = new SecurityContextImpl(auth);

        StepVerifier.create(kernel.get("/api/echo", Echo.class)
                        .contextWrite(c -> BusinessContextHolder.withContext(c, ctx))
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(
                                Mono.just(securityContext))))
                .assertNext(echo -> assertThat(echo.value()).isEqualTo("ok"))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/echo"))
                .withHeader("Authorization", equalTo("Bearer jwt-from-security")));
    }

    @Test
    @DisplayName("BusinessContext sans JWT → 403, pas d'appel oauth2/token")
    void contexte_sans_jwt_refuse_machine() {
        UUID tenant = UUID.randomUUID();
        wireMock.stubFor(post(urlMatching("/oauth2/token"))
                .willReturn(okJson("{\"access_token\":\"machine\"}")));

        BusinessContext ctx = new BusinessContext(tenant, null, Set.of(), null, "trace", Locale.FRENCH);

        StepVerifier.create(kernel.get("/api/echo", Echo.class)
                        .contextWrite(c -> BusinessContextHolder.withContext(c, ctx)))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) err).getStatus().value()).isEqualTo(403);
                    assertThat(err.getMessage()).contains("JWT délégué requis");
                })
                .verify();

        wireMock.verify(0, postRequestedFor(urlMatching("/oauth2/token")));
        verifyNoInteractions(tokenService);
    }

    @Test
    @DisplayName("BusinessContext scopé à un business (clé API, pas de JWT possible) → repli machine, pas 403")
    void contexte_scope_business_bascule_en_machine() {
        UUID tenant = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/echo"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"value\":\"ok\"},\"errorCode\":null}")));
        when(credentialStore.pourTenantCourant()).thenReturn(Mono.just(new KernelCreds("tenant-client", "tenant-secret")));
        when(tokenService.tokenPour("tenant-client", "tenant-secret")).thenReturn(Mono.just("machine-jwt"));

        // Authentifié via X-BC-Client-Id/X-BC-Api-Key (ApiKeyReactiveAuthenticationManager) : businessId
        // non nul, et par construction aucun JWT à déléguer — ce n'est pas une anomalie.
        BusinessContext ctx = new BusinessContext(tenant, null, Set.of(), businessId, "trace", Locale.FRENCH);

        StepVerifier.create(kernel.get("/api/echo", Echo.class)
                        .contextWrite(c -> BusinessContextHolder.withContext(c, ctx)))
                .assertNext(echo -> assertThat(echo.value()).isEqualTo("ok"))
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/echo"))
                .withHeader("Authorization", equalTo("Bearer machine-jwt"))
                .withHeader("X-Client-Id", equalTo("tenant-client"))
                .withHeader("X-Api-Key", equalTo("tenant-secret")));
    }

    @Test
    @DisplayName("erreur kernel 4xx → WebClientResponseException (pas de 502)")
    void erreur_4xx_relayee() {
        wireMock.stubFor(get(urlEqualTo("/api/conflict"))
                .willReturn(aResponse().withStatus(409).withBody("{\"errorCode\":\"CONFLICT\"}")));

        StepVerifier.create(kernel.get("/api/conflict", Echo.class)
                        .contextWrite(c -> KernelTokenHolder.withToken(c, "user-token")))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(WebClientResponseException.class);
                    assertThat(((WebClientResponseException) err).getStatusCode().value()).isEqualTo(409);
                })
                .verify();
    }

    @Test
    @DisplayName("erreur kernel 5xx → 502 Service kernel indisponible")
    void erreur_5xx_mappee_en_bad_gateway() {
        wireMock.stubFor(get(urlEqualTo("/api/down"))
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));

        StepVerifier.create(kernel.get("/api/down", Echo.class)
                        .contextWrite(c -> KernelTokenHolder.withToken(c, "user-token")))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) err).getStatus().value()).isEqualTo(502);
                    assertThat(err.getMessage()).contains("Kernel HTTP 503");
                })
                .verify();
    }
}
