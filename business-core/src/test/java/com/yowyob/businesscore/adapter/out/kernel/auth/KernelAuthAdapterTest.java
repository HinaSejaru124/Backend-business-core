package com.yowyob.businesscore.adapter.out.kernel.auth;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.infrastructure.config.AuthProperties;
import com.yowyob.businesscore.infrastructure.config.KernelProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration de l'adapter de login : appelle {@code POST /api/auth/login} en mode app-only
 * (X-Client-Id / X-Api-Key / X-Tenant-Id, sans Bearer), désenveloppe {@code data}, mappe le résultat,
 * et détecte OWNER. Kernel mocké par WireMock.
 */
class KernelAuthAdapterTest {

    private WireMockServer wireMock;
    private KernelAuthAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        KernelProperties kernelProps = new KernelProperties(
                "http://localhost:" + wireMock.port(), 5000, 0, "bc-app", "bc-secret", "ORGANIZATION", "OWNER");
        AuthProperties authProps = new AuthProperties("tenant-123", "", "");
        WebClient webClient = WebClient.builder().baseUrl(kernelProps.baseUrl()).build();
        adapter = new KernelAuthAdapter(webClient, kernelProps, authProps);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    @DisplayName("login : envoie les en-têtes app + tenant, désenveloppe data, détecte OWNER")
    void login_ok_owner() {
        wireMock.stubFor(post(urlEqualTo("/api/auth/login")).willReturn(okJson("""
                {"success":true,"data":{
                  "id":"u-1","tenantId":"tenant-123","actorId":"actor-9",
                  "accessToken":"jwt-rs256-xyz","tokenType":"Bearer","expiresInSeconds":900,
                  "authorities":["organizations:write","products:read"],
                  "organizations":[{"organizationId":"org-1","organizationCode":"ORG-001",
                                    "displayName":"Pharma du Centre","services":["PRODUCT","INVENTORY"]}]
                },"errorCode":null}""")));

        StepVerifier.create(adapter.login("user@email.com", "le-mot-de-passe"))
                .assertNext(r -> {
                    assertThat(r.accessToken()).isEqualTo("jwt-rs256-xyz");
                    assertThat(r.expiresInSeconds()).isEqualTo(900);
                    assertThat(r.estOwner()).isTrue();
                    assertThat(r.organisations()).hasSize(1);
                    assertThat(r.organisations().get(0).displayName()).isEqualTo("Pharma du Centre");
                    assertThat(r.actorId()).isEqualTo("actor-9");
                })
                .verifyComplete();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/auth/login"))
                .withHeader("X-Client-Id", equalTo("bc-app"))
                .withHeader("X-Api-Key", equalTo("bc-secret"))
                .withHeader("X-Tenant-Id", equalTo("tenant-123"))
                .withRequestBody(matchingJsonPath("$.principal", equalTo("user@email.com")))
                .withRequestBody(matchingJsonPath("$.password", equalTo("le-mot-de-passe"))));
    }

    @Test
    @DisplayName("login : identifiants invalides (401) → ProblemException 401")
    void login_401() {
        wireMock.stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse().withStatus(401).withBody("{}")));

        StepVerifier.create(adapter.login("user@email.com", "mauvais"))
                .expectErrorMatches(e -> e instanceof ProblemException pe && pe.getStatus().value() == 401)
                .verify();
    }
}
