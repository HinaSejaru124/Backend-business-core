package com.yowyob.businesscore.adapter.out.kernel.sales;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.spi.DepotActeur;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResoudreProduitEntreprise;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de la façade financière {@link EnregistrerVenteKernelAdapter} : vérifie l'enchaînement
 * des appels kernel (create order → confirm → facture client → bill cashier) et le mapping du résultat.
 * Kernel mocké par WireMock ; authentification et {@code ResolveurContexteKernel} mockés.
 */
class EnregistrerVenteKernelAdapterTest {

    private WireMockServer wireMock;
    private EnregistrerVenteKernelAdapter adapter;
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

        ResoudreProduitEntreprise resoudreProduit = mock(ResoudreProduitEntreprise.class);
        DepotActeur depotActeur = mock(DepotActeur.class);

        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(),
                mock(RequeteLogWriter.class), props);
        adapter = new EnregistrerVenteKernelAdapter(kernel, resolveur, resoudreProduit, depotActeur);
        this.resoudreProduit = resoudreProduit;
        this.depotActeur = depotActeur;
    }

    private ResoudreProduitEntreprise resoudreProduit;
    private DepotActeur depotActeur;

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("enchaîne order → confirm → facture client → bill cashier et mappe le billId + montant/devise")
    void facade_multi_appels() {
        String orderId = UUID.randomUUID().toString();
        String invoiceId = UUID.randomUUID().toString();
        String billId = UUID.randomUUID().toString();
        UUID offreId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID beneficiaireLocalId = UUID.randomUUID();
        UUID tiersKernelId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();

        when(resoudreProduit.resoudre(businessId, offreId)).thenReturn(Mono.just(productId));
        when(depotActeur.acteurParId(beneficiaireLocalId)).thenReturn(Mono.just(
                ActeurMetier.nouveau(beneficiaireLocalId, businessId, UUID.randomUUID(), tiersKernelId)));

        wireMock.stubFor(post(urlEqualTo("/api/sales/orders"))
                .willReturn(okJson("{\"id\":\"" + orderId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/sales/orders/" + orderId + "/confirm"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/api/accounting/invoices/from-orders/" + orderId))
                .willReturn(okJson("{\"id\":\"" + invoiceId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/bills/import/accounting-invoices/" + invoiceId))
                .willReturn(okJson("{\"id\":\"" + billId + "\",\"totalAmount\":1500,\"currency\":\"XAF\"}")));

        StepVerifier.create(adapter.enregistrer(offreId, 2, beneficiaireLocalId, businessId))
                .assertNext(vente -> {
                    assertThat(vente.commandeId().toString()).isEqualTo(orderId);
                    assertThat(vente.billId().toString()).isEqualTo(billId);
                    assertThat(vente.montant()).isEqualByComparingTo("1500");
                    assertThat(vente.devise()).isEqualTo("XAF");
                })
                .verifyComplete();

        // La commande porte la devise, l'organisation et les ids kernel résolus (pas les ids BC locaux).
        wireMock.verify(postRequestedFor(urlEqualTo("/api/sales/orders"))
                .withRequestBody(matchingJsonPath("$.currency", equalTo("XAF")))
                .withRequestBody(matchingJsonPath("$.organizationId", equalTo(organizationId.toString())))
                .withRequestBody(matchingJsonPath("$.productId", equalTo(productId.toString())))
                .withRequestBody(matchingJsonPath("$.customerThirdPartyId", equalTo(tiersKernelId.toString()))));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/sales/orders/" + orderId + "/confirm")));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/accounting/invoices/from-orders/" + orderId)));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/bills/import/accounting-invoices/" + invoiceId)));
    }

    @Test
    @DisplayName("annuler() appelle le cancel de la commande de vente (compensation)")
    void annuler_appelle_cancel() {
        UUID commandeId = UUID.randomUUID();
        wireMock.stubFor(post(urlPathEqualTo("/api/sales/orders/" + commandeId + "/cancel"))
                .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(adapter.annuler(commandeId)).verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/sales/orders/" + commandeId + "/cancel")));
    }
}
