package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import com.yowyob.businesscore.domain.shared.StatutType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
class TypeMetierPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry reg) {
        reg.add("spring.r2dbc.url", () -> String.format(
                "r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(),
                POSTGRES.getFirstMappedPort(),
                POSTGRES.getDatabaseName()));
        reg.add("spring.r2dbc.username", POSTGRES::getUsername);
        reg.add("spring.r2dbc.password", POSTGRES::getPassword);
        reg.add("spring.liquibase.url",      POSTGRES::getJdbcUrl);
        reg.add("spring.liquibase.user",     POSTGRES::getUsername);
        reg.add("spring.liquibase.password", POSTGRES::getPassword);
    }

    @Autowired
    TypeMetierPersistenceAdapter adapter;

    private static final UUID TENANT = UUID.randomUUID();

    @Test
    @DisplayName("sauvegarder puis trouverParId doit retourner le même type")
    void sauvegarder_et_retrouver() {
        TypeMetier type = TypeMetier.creer(TENANT, "RESTO", "Restaurant", null);

        StepVerifier.create(
                adapter.sauvegarder(type)
                       .flatMap(saved -> adapter.trouverParId(saved.id()))
        )
        .assertNext(found -> {
            assertThat(found.code()).isEqualTo("RESTO");
            assertThat(found.statut()).isEqualTo(StatutType.BROUILLON);
            assertThat(found.tenantId()).isEqualTo(TENANT);
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("existeParCodeEtTenant doit retourner true si le code existe")
    void existe_par_code_et_tenant_true() {
        TypeMetier type = TypeMetier.creer(TENANT, "COIFF", "Coiffeur", null);

        StepVerifier.create(
                adapter.sauvegarder(type)
                       .then(adapter.existeParCodeEtTenant("COIFF", TENANT))
        )
        .expectNext(true)
        .verifyComplete();
    }

    @Test
    @DisplayName("existeParCodeEtTenant doit retourner false pour un code inconnu")
    void existe_par_code_et_tenant_false() {
        StepVerifier.create(
                adapter.existeParCodeEtTenant("INCONNU_" + UUID.randomUUID(), TENANT)
        )
        .expectNext(false)
        .verifyComplete();
    }

    @Test
    @DisplayName("sauvegarder doit persister le statut PUBLIE après publication")
    void sauvegarder_statut_publie() {
        TypeMetier publie = TypeMetier.creer(TENANT, "AVOCAT", "Cabinet d'avocats", null)
                .publier();

        StepVerifier.create(
                adapter.sauvegarder(publie)
                       .flatMap(saved -> adapter.trouverParId(saved.id()))
        )
        .assertNext(found ->
            assertThat(found.statut()).isEqualTo(StatutType.PUBLIE)
        )
        .verifyComplete();
    }
}
