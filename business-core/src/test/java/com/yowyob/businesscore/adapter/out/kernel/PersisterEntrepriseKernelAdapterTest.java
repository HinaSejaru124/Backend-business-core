package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.kernel.organization.PersisterEntrepriseKernelAdapter;
import com.yowyob.businesscore.domain.shared.CycleVie;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThat;
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
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER");
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(), props);
        adapter = new PersisterEntrepriseKernelAdapter(kernel, props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void cree_le_business_actor_puis_l_organisation() {
        UUID businessActorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/actors/onboarding"))
                .willReturn(okJson("{\"id\":\"" + businessActorId + "\"}")));
        wireMock.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"id\":\"" + orgId + "\"}")));

        StepVerifier.create(adapter.creerOrganisation("Pharma Yaoundé"))
                .assertNext(prov -> {
                    assertThat(prov.organizationId()).isEqualTo(orgId);
                    assertThat(prov.businessActorId()).isEqualTo(businessActorId);
                })
                .verifyComplete();

        // Le business actor propriétaire est créé d'abord (rôle OWNER).
        wireMock.verify(postRequestedFor(urlEqualTo("/api/actors/onboarding"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Pharma Yaoundé")))
                .withRequestBody(matchingJsonPath("$.role", equalTo("OWNER"))));
        // Puis l'organisation avec le payload complet exigé par le kernel.
        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations"))
                .withRequestBody(matchingJsonPath("$.businessActorId", equalTo(businessActorId.toString())))
                .withRequestBody(matchingJsonPath("$.service", equalTo("BUSINESS_CORE")))
                .withRequestBody(matchingJsonPath("$.shortName", equalTo("Pharma Yaoundé")))
                .withRequestBody(matchingJsonPath("$.longName", equalTo("Pharma Yaoundé")))
                .withRequestBody(matchingJsonPath("$.code")));
    }

    @Test
    void desenveloppe_les_reponses_enveloppees_du_kernel() {
        UUID businessActorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        // Réponses au format réel du kernel : { success, data: {...}, message, errorCode }.
        wireMock.stubFor(post(urlEqualTo("/api/actors/onboarding"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + businessActorId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));
        wireMock.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + orgId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));

        StepVerifier.create(adapter.creerOrganisation("Pharma Yaoundé"))
                .assertNext(prov -> assertThat(prov.organizationId()).isEqualTo(orgId))
                .verifyComplete();
    }

    @Test
    void errorCode_non_nul_remonte_une_erreur_meme_en_http_200() {
        // HTTP 200 mais erreur métier dans l'enveloppe → doit échouer (cf. §0.3).
        wireMock.stubFor(post(urlEqualTo("/api/actors/onboarding"))
                .willReturn(okJson("{\"success\":false,\"data\":null,"
                        + "\"message\":\"acteur invalide\",\"errorCode\":\"ACTOR_INVALID\"}")));

        StepVerifier.create(adapter.creerOrganisation("Pharma Yaoundé"))
                .expectError(KernelException.class)
                .verify();
    }

    @Test
    void cycle_de_vie_mappe_sur_les_transitions_kernel() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/suspend"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/close"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/reopen"))
                .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(adapter.changerCycleVieKernel(orgId, CycleVie.SUSPENDUE)).verifyComplete();
        StepVerifier.create(adapter.changerCycleVieKernel(orgId, CycleVie.FERMEE)).verifyComplete();
        StepVerifier.create(adapter.changerCycleVieKernel(orgId, CycleVie.ACTIVE)).verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/suspend")));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/close")));
        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/reopen")));
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
