package com.yowyob.businesscore.adapter.out.kernel.inventory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de {@link VerifierDisponibiliteKernelAdapter} : vérifie que la lecture du solde
 * envoie bien les TROIS query params exigés par le kernel (organizationId, agencyId, productId), les
 * deux premiers résolus par le {@link ResolveurContexteKernel}.
 */
class VerifierDisponibiliteKernelAdapterTest {

    private WireMockServer wireMock;
    private VerifierDisponibiliteKernelAdapter adapter;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID agencyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        KernelTokenService tokenService = mock(KernelTokenService.class);
        KernelCredentialStore credentialStore = mock(KernelCredentialStore.class);
        when(credentialStore.pourTenantCourant())
                .thenReturn(Mono.just(new KernelCredentialStore.KernelCreds("client", "secret")));
        when(tokenService.tokenPour(any(), any())).thenReturn(Mono.just("jwt-test"));

        ResolveurContexteKernel resolveur = mock(ResolveurContexteKernel.class);
        when(resolveur.resoudre(any())).thenReturn(Mono.just(new ContexteKernel(
                organizationId, agencyId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "XAF")));

        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(),
                mock(RequeteLogWriter.class), props);
        adapter = new VerifierDisponibiliteKernelAdapter(kernel, resolveur);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("soldeStock envoie organizationId + agencyId + productId et mappe le solde")
    void solde_envoie_trois_params() {
        UUID productId = UUID.randomUUID();
        wireMock.stubFor(get(urlPathEqualTo("/api/inventory/movements/balance"))
                .willReturn(okJson("{\"balance\":42}")));

        StepVerifier.create(adapter.soldeStock(productId, UUID.randomUUID()))
                .expectNext(42L)
                .verifyComplete();

        wireMock.verify(getRequestedFor(urlPathEqualTo("/api/inventory/movements/balance"))
                .withQueryParam("organizationId", equalTo(organizationId.toString()))
                .withQueryParam("agencyId", equalTo(agencyId.toString()))
                .withQueryParam("productId", equalTo(productId.toString())));
    }
}
