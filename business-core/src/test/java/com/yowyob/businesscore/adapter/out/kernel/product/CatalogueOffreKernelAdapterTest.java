package com.yowyob.businesscore.adapter.out.kernel.product;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.domain.port.out.CreationProduit;
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
 * Test d'intégration de {@link CatalogueOffreKernelAdapter} : vérifie que la création de produit envoie
 * bien tous les champs obligatoires de {@code CreateProductRequest} (organizationId, sku, familyCode,
 * variantLabel, unitPrice, currency).
 */
class CatalogueOffreKernelAdapterTest {

    private WireMockServer wireMock;
    private CatalogueOffreKernelAdapter adapter;

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
        adapter = new CatalogueOffreKernelAdapter(kernel);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("creerProduit envoie les champs obligatoires du CreateProductRequest")
    void creer_produit_envoie_champs_obligatoires() {
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/products"))
                .willReturn(okJson("{\"id\":\"" + productId + "\"}")));

        CreationProduit demande = new CreationProduit(
                organizationId, "OFFRE-GAZ-12345678", "Bouteille de gaz", "GENERAL", "STANDARD",
                new BigDecimal("5000"), "XAF", true, false);

        StepVerifier.create(adapter.creerProduit(demande))
                .expectNext(productId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/products"))
                .withRequestBody(matchingJsonPath("$.organizationId", equalTo(organizationId.toString())))
                .withRequestBody(matchingJsonPath("$.sku", equalTo("OFFRE-GAZ-12345678")))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Bouteille de gaz")))
                .withRequestBody(matchingJsonPath("$.familyCode", equalTo("GENERAL")))
                .withRequestBody(matchingJsonPath("$.variantLabel", equalTo("STANDARD")))
                .withRequestBody(matchingJsonPath("$.unitPrice", equalTo("5000")))
                .withRequestBody(matchingJsonPath("$.currency", equalTo("XAF")))
                .withRequestBody(matchingJsonPath("$.unitPrice", equalTo("5000"))));
    }
}
