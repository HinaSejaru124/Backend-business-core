package com.yowyob.businesscore.adapter.out.kernel.inventory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
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
 * Test d'intégration de {@link EngagerStockKernelAdapter} : la sortie de vente crée un mouvement OUT
 * puis le valide (engagement en 2 temps) ; la compensation crée un mouvement IN.
 */
class EngagerStockKernelAdapterTest {

    private WireMockServer wireMock;
    private EngagerStockKernelAdapter adapter;
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
                organizationId, agencyId, UUID.randomUUID(), null, null, "XAF")));

        KernelProperties props = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(), props);
        adapter = new EngagerStockKernelAdapter(kernel, resolveur);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("sortieVente crée un mouvement OUT puis le valide")
    void sortie_vente_cree_et_valide() {
        UUID productId = UUID.randomUUID();
        UUID commandeId = UUID.randomUUID();
        UUID mouvementId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/inventory/movements"))
                .willReturn(okJson("{\"id\":\"" + mouvementId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/inventory/movements/" + mouvementId + "/validate"))
                .willReturn(okJson("{}")));

        StepVerifier.create(adapter.sortieVente(productId, 3, UUID.randomUUID(), commandeId))
                .expectNext(mouvementId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/inventory/movements"))
                .withRequestBody(matchingJsonPath("$.organizationId", equalTo(organizationId.toString())))
                .withRequestBody(matchingJsonPath("$.agencyId", equalTo(agencyId.toString())))
                .withRequestBody(matchingJsonPath("$.productId", equalTo(productId.toString())))
                .withRequestBody(matchingJsonPath("$.movementType", equalTo("OUT")))
                .withRequestBody(matchingJsonPath("$.quantity", equalTo("3")))
                .withRequestBody(matchingJsonPath("$.referenceNumber", equalTo(commandeId.toString()))));
        wireMock.verify(postRequestedFor(
                urlEqualTo("/api/inventory/movements/" + mouvementId + "/validate")));
    }

    @Test
    @DisplayName("annulerSortie compense par un mouvement IN de la même quantité")
    void annuler_sortie_compense_en_entree() {
        UUID productId = UUID.randomUUID();
        UUID mouvementId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/inventory/movements"))
                .willReturn(okJson("{\"id\":\"" + mouvementId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/inventory/movements/" + mouvementId + "/validate"))
                .willReturn(okJson("{}")));

        StepVerifier.create(adapter.annulerSortie(productId, 3, UUID.randomUUID(), UUID.randomUUID()))
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/inventory/movements"))
                .withRequestBody(matchingJsonPath("$.movementType", equalTo("IN"))));
    }
}
