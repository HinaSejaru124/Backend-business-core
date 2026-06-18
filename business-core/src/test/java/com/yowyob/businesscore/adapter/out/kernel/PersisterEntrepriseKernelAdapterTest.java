package com.yowyob.businesscore.adapter.out.kernel;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.yowyob.businesscore.adapter.out.kernel.organization.PersisterEntrepriseKernelAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import reactor.test.StepVerifier;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

class PersisterEntrepriseKernelAdapterTest {

    @RegisterExtension
    static WireMockExtension kernel = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    @Test
    void cree_l_organisation_puis_l_agence_par_defaut() {
        UUID orgId = UUID.randomUUID();

        kernel.stubFor(post(urlEqualTo("/api/organizations"))
                .willReturn(okJson("{\"id\":\"" + orgId + "\"}")));
        kernel.stubFor(post(urlEqualTo("/api/organizations/" + orgId + "/agencies"))
                .willReturn(okJson("{\"id\":\"" + UUID.randomUUID() + "\"}")));

        PersisterEntrepriseKernelAdapter adapter =
                new PersisterEntrepriseKernelAdapter(new WebClientKernelClient(kernel.baseUrl()));

        StepVerifier.create(adapter.creerOrganisationAvecAgence("Pharma Yaoundé"))
                .expectNext(orgId)
                .verifyComplete();

        // L'ordre et les deux appels kernel sont vérifiés.
        kernel.verify(postRequestedFor(urlEqualTo("/api/organizations")));
        kernel.verify(postRequestedFor(urlEqualTo("/api/organizations/" + orgId + "/agencies"))
                .withHeader("X-Organization-Id", equalTo(orgId.toString())));
    }
}
