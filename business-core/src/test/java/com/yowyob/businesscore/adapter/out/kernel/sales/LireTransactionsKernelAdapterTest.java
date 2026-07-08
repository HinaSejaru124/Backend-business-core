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
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LireTransactionsKernelAdapterTest {

    private WireMockServer wireMock;
    private LireTransactionsKernelAdapter adapter;
    private final UUID organizationId = UUID.randomUUID();

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
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(), props);
        adapter = new LireTransactionsKernelAdapter(kernel);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("liste les mouvements de caisse via /api/cashier/movements")
    void liste_mouvements() {
        UUID movementId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/cashier/movements"))
                .willReturn(okJson("[{\"id\":\"" + movementId + "\",\"amount\":1500,"
                        + "\"currency\":\"XAF\",\"status\":\"POSTED\","
                        + "\"createdAt\":\"2026-07-08T10:00:00Z\"}]")));

        StepVerifier.create(adapter.listerParOrganisation(organizationId, 0, 20))
                .assertNext(vue -> {
                    assertThat(vue.transactionKernelId()).isEqualTo(movementId);
                    assertThat(vue.montant()).isEqualByComparingTo(new BigDecimal("1500"));
                    assertThat(vue.devise()).isEqualTo("XAF");
                    assertThat(vue.statut()).isEqualTo("POSTED");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("détail bill via /api/cashier/bills/{id}")
    void trouve_bill() {
        UUID billId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/cashier/bills/" + billId))
                .willReturn(okJson("{\"id\":\"" + billId + "\",\"totalAmount\":2000,"
                        + "\"paidAmount\":500,\"currency\":\"XAF\",\"status\":\"PARTIAL\"}")));

        StepVerifier.create(adapter.trouverBill(organizationId, billId))
                .assertNext(vue -> {
                    assertThat(vue.montant()).isEqualByComparingTo(new BigDecimal("2000"));
                    assertThat(vue.montantPaye()).isEqualByComparingTo(new BigDecimal("500"));
                })
                .verifyComplete();
    }
}
