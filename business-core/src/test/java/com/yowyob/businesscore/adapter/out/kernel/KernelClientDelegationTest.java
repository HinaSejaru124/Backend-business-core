package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.security.KernelTokenHolder;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Vérifie le flux <b>délégué</b> de KernelClient : quand un token utilisateur est présent dans le Reactor
 * Context ({@link KernelTokenHolder}), l'appel kernel porte ce Bearer + {@code X-Tenant-Id} (du tenant
 * courant) + l'identité d'application BC ({@code X-Client-Id}/{@code X-Api-Key} de config).
 */
class KernelClientDelegationTest {

    private WireMockServer wireMock;
    private KernelClient kernel;

    public record Echo(String value) {
    }

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "bc-app", "bc-secret", "ORGANIZATION", "OWNER");
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        kernel = new KernelClient(
                webClient, mock(KernelTokenService.class), mock(KernelCredentialStore.class),
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
}
