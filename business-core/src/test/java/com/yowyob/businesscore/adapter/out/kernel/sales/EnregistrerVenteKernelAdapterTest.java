package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de la façade financière {@link EnregistrerVenteKernelAdapter} : vérifie l'enchaînement
 * des trois appels kernel (create order → confirm → read bill) et le mapping du résultat. Kernel mické
 * par WireMock ; l'authentification (token + credentials) est mickée.
 */
class EnregistrerVenteKernelAdapterTest {

    private WireMockServer wireMock;
    private EnregistrerVenteKernelAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        KernelTokenService tokenService = mock(KernelTokenService.class);
        KernelCredentialStore credentialStore = mock(KernelCredentialStore.class);
        when(credentialStore.pourTenantCourant())
                .thenReturn(Mono.just(new KernelCredentialStore.KernelCreds("client", "secret")));
        when(tokenService.tokenPour(any(), any())).thenReturn(Mono.just("jwt-test"));

        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "");
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(webClient, tokenService, credentialStore, props);
        adapter = new EnregistrerVenteKernelAdapter(kernel);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("enchaîne create order, confirm puis read bill et mappe la transaction")
    void facade_multi_appels() {
        String orderId = UUID.randomUUID().toString();
        String billId = UUID.randomUUID().toString();

        wireMock.stubFor(post(urlEqualTo("/api/sales/orders"))
                .willReturn(okJson("{\"id\":\"" + orderId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/sales/orders/" + orderId + "/confirm"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(get(urlEqualTo("/api/cashier/bills/" + orderId))
                .willReturn(okJson("{\"id\":\"" + billId + "\",\"amount\":1500,\"currency\":\"XAF\"}")));

        StepVerifier.create(adapter.enregistrer(UUID.randomUUID(), 2, UUID.randomUUID()))
                .assertNext(vente -> {
                    assertThat(vente.transactionKernelId().toString()).isEqualTo(billId);
                    assertThat(vente.montant()).isEqualByComparingTo("1500");
                    assertThat(vente.devise()).isEqualTo("XAF");
                })
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/sales/orders")));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/sales/orders/" + orderId + "/confirm")));
        wireMock.verify(getRequestedFor(urlEqualTo("/api/cashier/bills/" + orderId)));
    }
}
