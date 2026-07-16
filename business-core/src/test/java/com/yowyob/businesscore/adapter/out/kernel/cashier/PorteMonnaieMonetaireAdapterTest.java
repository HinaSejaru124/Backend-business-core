package com.yowyob.businesscore.adapter.out.kernel.cashier;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.application.error.ProblemException;
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

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de {@link PorteMonnaieMonetaireAdapter} : l'encaissement règle le bill cashier
 * de la vente via {@code POST /api/bills/pay}, avec la caisse ({@code registerId}) résolue par le socle.
 */
class PorteMonnaieMonetaireAdapterTest {

    private WireMockServer wireMock;
    private PorteMonnaieMonetaireAdapter adapter;
    private final UUID organizationId = UUID.randomUUID();
    private final UUID registerId = UUID.randomUUID();

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
                organizationId, UUID.randomUUID(), UUID.randomUUID(), registerId, UUID.randomUUID(), "XAF")));

        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(),
                mock(RequeteLogWriter.class), props);
        SessionCaisseKernelSupport sessionCaisse = new SessionCaisseKernelSupport(kernel);
        adapter = new PorteMonnaieMonetaireAdapter(kernel, resolveur, sessionCaisse);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("règle le bill cashier via bills/pay (billId en query param) avec la session ouverte")
    void encaisse_le_bill() {
        UUID billId = UUID.randomUUID();
        UUID paiementId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        // Une session est déjà ouverte pour la caisse : elle est réutilisée, pas de POST /api/sessions.
        wireMock.stubFor(get(urlEqualTo("/api/cashier/sessions"))
                .willReturn(okJson("[{\"id\":\"" + sessionId + "\",\"registerId\":\"" + registerId
                        + "\",\"status\":\"OPEN\"}]")));
        wireMock.stubFor(post(urlPathEqualTo("/api/bills/pay"))
                .withQueryParam("billId", equalTo(billId.toString()))
                .willReturn(okJson("{\"id\":\"" + paiementId + "\"}")));

        StepVerifier.create(adapter.enregistrerEchange(
                        billId, new BigDecimal("1500"), "XAF", UUID.randomUUID()))
                .expectNext(paiementId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/bills/pay"))
                .withQueryParam("billId", equalTo(billId.toString()))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("1500")))
                .withRequestBody(matchingJsonPath("$.registerId", equalTo(registerId.toString())))
                .withRequestBody(matchingJsonPath("$.sessionId", equalTo(sessionId.toString()))));
    }

    @Test
    @DisplayName("ouvre une session de caisse quand aucune n'est ouverte, puis encaisse")
    void ouvre_session_puis_encaisse() {
        UUID billId = UUID.randomUUID();
        UUID paiementId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/cashier/sessions"))
                .willReturn(okJson("[]")));
        wireMock.stubFor(post(urlEqualTo("/api/sessions"))
                .willReturn(okJson("{\"id\":\"" + sessionId + "\",\"registerId\":\"" + registerId
                        + "\",\"status\":\"OPEN\"}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/bills/pay"))
                .withQueryParam("billId", equalTo(billId.toString()))
                .willReturn(okJson("{\"id\":\"" + paiementId + "\"}")));

        StepVerifier.create(adapter.enregistrerEchange(
                        billId, new BigDecimal("1500"), "XAF", UUID.randomUUID()))
                .expectNext(paiementId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/sessions"))
                .withRequestBody(matchingJsonPath("$.registerId", equalTo(registerId.toString())))
                .withRequestBody(matchingJsonPath("$.currency", equalTo("XAF"))));
        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/bills/pay"))
                .withRequestBody(matchingJsonPath("$.sessionId", equalTo(sessionId.toString()))));
    }

    @Test
    @DisplayName("billId manquant → erreur 422 (pas d'appel kernel à l'aveugle)")
    void bill_manquant_echoue() {
        StepVerifier.create(adapter.enregistrerEchange(
                        null, new BigDecimal("100"), "XAF", UUID.randomUUID()))
                .expectError(ProblemException.class)
                .verify();
    }
}
