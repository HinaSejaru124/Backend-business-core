package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.kernel.organization.PersisterEntrepriseKernelAdapter;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test d'intégration de {@link PersisterEntrepriseKernelAdapter} : vérifie la création de l'organisation
 * et de son agence (avec l'en-tête {@code X-Organization-Id}). Kernel mické par WireMock ;
 * l'authentification (token + credentials) est mickée.
 */
class PersisterEntrepriseKernelAdapterTest {

    private WireMockServer wireMock;
    private PersisterEntrepriseKernelAdapter adapter;

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
        adapter = new PersisterEntrepriseKernelAdapter(kernel);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void cree_l_organisation() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"id\":\"" + orgId + "\"}")));

        StepVerifier.create(adapter.creerOrganisation("Pharma Yaoundé"))
                .expectNext(orgId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations")));
    }

    @Test
    void cree_l_agence_avec_entete_organisation() {
        UUID orgId = UUID.randomUUID();
        UUID agenceId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/agencies"))
                .willReturn(okJson("{\"id\":\"" + agenceId + "\"}")));

        StepVerifier.create(adapter.creerAgence(orgId, "Pharma Yaoundé — agence principale"))
                .expectNext(agenceId)
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/agencies"))
                .withHeader("X-Organization-Id", equalTo(orgId.toString())));
    }
}
