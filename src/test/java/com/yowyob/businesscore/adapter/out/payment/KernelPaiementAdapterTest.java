package com.yowyob.businesscore.adapter.out.payment;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.domain.port.out.PaiementPort.DemandePaiement;
import com.yowyob.businesscore.domain.port.out.PaiementPort.ResultatPaiement;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import com.yowyob.businesscore.infrastructure.config.PaymentProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de {@link KernelPaiementAdapter} : ouverture d'un ordre de paiement mobile money et
 * mapping du statut kernel vers l'issue métier ({@link ResultatPaiement.Statut}).
 */
class KernelPaiementAdapterTest {

    private WireMockServer wireMock;
    private KernelPaiementAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        KernelTokenService tokenService = mock(KernelTokenService.class);
        KernelCredentialStore credentialStore = mock(KernelCredentialStore.class);
        when(credentialStore.pourTenantCourant())
                .thenReturn(Mono.just(new KernelCredentialStore.KernelCreds("client", "secret")));
        when(tokenService.tokenPour(any(), any())).thenReturn(Mono.just("jwt-test"));

        KernelProperties kProps = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(kProps.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(),
                mock(RequeteLogWriter.class), kProps);

        PaymentProperties pProps = new PaymentProperties(null, null, null, null, null, null);
        adapter = new KernelPaiementAdapter(kernel, pProps);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    private static String ordreJson(String id, String status, String redirectUrl) {
        String redirect = redirectUrl == null ? "null" : "\"" + redirectUrl + "\"";
        return "{\"success\":true,\"data\":{\"id\":\"" + id + "\",\"status\":\"" + status
                + "\",\"redirectUrl\":" + redirect + "},\"message\":\"ok\",\"errorCode\":null}";
    }

    @Test
    @DisplayName("demanderPaiement ouvre un ordre et renvoie EN_ATTENTE + urlPaiement quand PENDING")
    void demander_paiement_pending() {
        UUID orderId = UUID.randomUUID();
        String redirect = "https://my-coolpay.com/payment/checkout/" + orderId;
        wireMock.stubFor(post(urlEqualTo("/api/payments/orders"))
                .willReturn(okJson(ordreJson(orderId.toString(), "PENDING", redirect))));

        DemandePaiement demande = new DemandePaiement(
                UUID.randomUUID(), "FREE", "PRO", 15000, "XAF", "692162333");

        StepVerifier.create(adapter.demanderPaiement(demande))
                .expectNext(new ResultatPaiement(ResultatPaiement.Statut.EN_ATTENTE, redirect, orderId.toString()))
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/payments/orders"))
                .withRequestBody(matchingJsonPath("$.clientId", equalTo("business-core")))
                .withRequestBody(matchingJsonPath("$.serviceCode", equalTo("PLAN_UPGRADE_PRO")))
                .withRequestBody(matchingJsonPath("$.amount", equalTo("15000")))
                .withRequestBody(matchingJsonPath("$.currency", equalTo("XAF")))
                .withRequestBody(matchingJsonPath("$.provider", equalTo("MYCOOLPAY")))
                .withRequestBody(matchingJsonPath("$.method", equalTo("MOBILE_MONEY")))
                .withRequestBody(matchingJsonPath("$.payerReference", equalTo("692162333")))
                .withRequestBody(matchingJsonPath("$.idempotencyKey")));
    }

    @Test
    @DisplayName("verifierStatut mappe SUCCESS -> CONFIRME")
    void verifier_statut_succes() {
        String orderId = UUID.randomUUID().toString();
        wireMock.stubFor(post(urlEqualTo("/api/payments/orders/" + orderId + "/refresh"))
                .willReturn(okJson(ordreJson(orderId, "SUCCESS", null))));

        StepVerifier.create(adapter.verifierStatut(orderId))
                .expectNext(new ResultatPaiement(ResultatPaiement.Statut.CONFIRME, null, orderId))
                .verifyComplete();
    }

    @Test
    @DisplayName("verifierStatut mappe FAILED -> REFUSE")
    void verifier_statut_echec() {
        String orderId = UUID.randomUUID().toString();
        wireMock.stubFor(post(urlEqualTo("/api/payments/orders/" + orderId + "/refresh"))
                .willReturn(okJson(ordreJson(orderId, "FAILED", null))));

        StepVerifier.create(adapter.verifierStatut(orderId))
                .expectNext(new ResultatPaiement(ResultatPaiement.Statut.REFUSE, null, orderId))
                .verifyComplete();
    }

    @Test
    @DisplayName("verifierStatut : statut inconnu -> EN_ATTENTE (fail-safe, aucun déblocage)")
    void verifier_statut_inconnu() {
        String orderId = UUID.randomUUID().toString();
        wireMock.stubFor(post(urlEqualTo("/api/payments/orders/" + orderId + "/refresh"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(ordreJson(orderId, "SOMETHING_NEW", null))));

        StepVerifier.create(adapter.verifierStatut(orderId))
                .expectNext(new ResultatPaiement(ResultatPaiement.Statut.EN_ATTENTE, null, orderId))
                .verifyComplete();
    }
}
