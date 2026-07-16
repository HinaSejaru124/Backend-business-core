package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelCredentialStore;
import com.yowyob.businesscore.adapter.out.kernel.auth.KernelTokenService;
import com.yowyob.businesscore.adapter.out.kernel.organization.PersisterEntrepriseKernelAdapter;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.domain.shared.CycleVie;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
                "http://localhost:" + wireMock.port(), 5000, 0, "", "", "BUSINESS_CORE", "OWNER", null);
        WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
        KernelClient kernel = new KernelClient(
                webClient, tokenService, credentialStore, JsonMapper.builder().build(),
                mock(RequeteLogWriter.class), props);
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
        wireMock.stubFor(get(urlEqualTo("/api/actors/me"))
                .willReturn(aResponse().withStatus(404)));
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
    void passe_directement_a_l_organisation_si_actor_deja_onboarde() {
        UUID businessActorId = UUID.fromString("04b12f1d-aed5-4802-baf3-b5985bbb2c43");
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/actors/me"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + businessActorId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));
        wireMock.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + orgId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));

        StepVerifier.create(adapter.creerOrganisation("Boutique Alpha"))
                .assertNext(prov -> {
                    assertThat(prov.businessActorId()).isEqualTo(businessActorId);
                    assertThat(prov.organizationId()).isEqualTo(orgId);
                })
                .verifyComplete();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/actors/me")));
        wireMock.verify(0, postRequestedFor(urlEqualTo("/api/actors/onboarding")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/organizations"))
                .withRequestBody(matchingJsonPath("$.businessActorId", equalTo(businessActorId.toString()))));
    }

    @Test
    void onboard_si_actor_absent_puis_cree_l_organisation() {
        UUID businessActorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/actors/me"))
                .willReturn(aResponse().withStatus(404)));
        wireMock.stubFor(post(urlEqualTo("/api/actors/onboarding"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + businessActorId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));
        wireMock.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"success\":true,\"data\":{\"id\":\"" + orgId
                        + "\"},\"message\":\"ok\",\"errorCode\":null}")));

        StepVerifier.create(adapter.creerOrganisation("Boutique Alpha"))
                .assertNext(prov -> assertThat(prov.organizationId()).isEqualTo(orgId))
                .verifyComplete();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/actors/me")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/actors/onboarding")));
    }

    @Test
    void desenveloppe_les_reponses_enveloppees_du_kernel() {
        UUID businessActorId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/actors/me"))
                .willReturn(aResponse().withStatus(404)));
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
        wireMock.stubFor(get(urlEqualTo("/api/actors/me"))
                .willReturn(aResponse().withStatus(404)));
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
    void approuve_l_organisation_avec_reason() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/approve"))
                .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(adapter.approuverOrganisation(orgId, "Validation KYC")).verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/approve"))
                .withRequestBody(matchingJsonPath("$.reason", equalTo("Validation KYC"))));
    }

    @Test
    void approuve_utilise_le_motif_par_defaut_si_reason_vide() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/approve"))
                .willReturn(aResponse().withStatus(200)));

        StepVerifier.create(adapter.approuverOrganisation(orgId, "  ")).verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/approve"))
                .withRequestBody(matchingJsonPath("$.reason", equalTo("Approbation initiale"))));
    }

    @Test
    void approuve_ignore_si_deja_approuvee_409() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/approve"))
                .willReturn(aResponse().withStatus(409).withBody("{\"errorCode\":\"ALREADY_APPROVED\"}")));

        StepVerifier.create(adapter.approuverOrganisation(orgId, "Validation")).verifyComplete();

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/approve")));
    }

    @Test
    void souscrit_les_services_kernel() {
        UUID orgId = UUID.randomUUID();
        wireMock.stubFor(get(urlEqualTo("/api/organizations/services/catalog"))
                .willReturn(okJson("""
                        {"success":true,"data":[
                          {"code":"COMMERCIAL","requiredDependencies":[]},
                          {"code":"ACCOUNTING","requiredDependencies":[]},
                          {"code":"BILLING","requiredDependencies":["COMMERCIAL"]},
                          {"code":"CASHIER","requiredDependencies":["ACCOUNTING"]}
                        ]}
                        """)));
        wireMock.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/services"))
                .willReturn(okJson("{}")));

        StepVerifier.create(adapter.souscrireServices(orgId)).verifyComplete();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/api/organizations/services/catalog")));
        wireMock.verify(4, postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/services")));

        List<String> codes = wireMock.findAll(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/services")))
                .stream()
                .map(req -> JsonMapper.builder().build()
                        .readTree(req.getBodyAsString())
                        .get("serviceCode")
                        .asString())
                .toList();
        assertThat(codes).containsExactly("COMMERCIAL", "ACCOUNTING", "BILLING", "CASHIER");
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

        wireMock.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/agencies")));
    }
}
