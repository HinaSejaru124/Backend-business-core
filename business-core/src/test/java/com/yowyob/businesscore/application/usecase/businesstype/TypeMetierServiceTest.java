package com.yowyob.businesscore.application.usecase.businesstype;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import com.yowyob.businesscore.domain.port.out.PersisterTypeMetier;
import com.yowyob.businesscore.domain.port.out.ResoudreBusinessDomain;
import com.yowyob.businesscore.domain.shared.StatutType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TypeMetierServiceTest {

    @Mock PersisterTypeMetier    depot;
    @Mock ResoudreBusinessDomain resoudreBusinessDomain;

    TypeMetierService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TYPE_ID   = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT_ID, null, Set.of(), null, "trace-test", Locale.FRENCH);

    @BeforeEach
    void setUp() {
        service = new TypeMetierService(depot, resoudreBusinessDomain);
    }

    @Nested
    @DisplayName("creer()")
    class Creer {

        @Test
        @DisplayName("doit créer un type quand le code est libre")
        void creer_ok_code_libre() {
            TypeMetier attendu = TypeMetier.creer(TENANT_ID, "PHARMA", "Pharmacie", null);

            when(depot.existeParCodeEtTenant("PHARMA", TENANT_ID))
                    .thenReturn(Mono.just(false));
            when(depot.sauvegarder(any()))
                    .thenReturn(Mono.just(attendu));

            StepVerifier.create(service.creer("PHARMA", "Pharmacie", null, null, ctx))
                    .assertNext(t -> {
                        assertThat(t.code()).isEqualTo("PHARMA");
                        assertThat(t.statut()).isEqualTo(StatutType.BROUILLON);
                        assertThat(t.tenantId()).isEqualTo(TENANT_ID);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("doit rejeter si le code existe déjà dans le tenant")
        void creer_rejette_code_duplique() {
            when(depot.existeParCodeEtTenant("PHARMA", TENANT_ID))
                    .thenReturn(Mono.just(true));

            StepVerifier.create(service.creer("PHARMA", "Pharmacie", null, null, ctx))
                    .expectErrorMatches(e ->
                            e instanceof ProblemException pe &&
                            pe.getDetail().contains("PHARMA"))
                    .verify();
        }

        @Test
        @DisplayName("doit résoudre le BusinessDomain kernel si domainCode est fourni")
        void creer_avec_domain_code_appelle_kernel() {
            UUID domainId = UUID.randomUUID();
            TypeMetier attendu = TypeMetier.creer(TENANT_ID, "PHARMA", "Pharmacie", domainId);

            when(depot.existeParCodeEtTenant("PHARMA", TENANT_ID))
                    .thenReturn(Mono.just(false));
            when(resoudreBusinessDomain.resoudreOuCreer("SANTE", "Santé"))
                    .thenReturn(Mono.just(domainId));
            when(depot.sauvegarder(any()))
                    .thenReturn(Mono.just(attendu));

            StepVerifier.create(service.creer("PHARMA", "Pharmacie", "SANTE", "Santé", ctx))
                    .assertNext(t -> assertThat(t.businessDomainId()).isEqualTo(domainId))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("publier()")
    class Publier {

        @Test
        @DisplayName("doit publier un type BROUILLON du bon tenant")
        void publier_ok() {
            TypeMetier brouillon = TypeMetier.creer(TENANT_ID, "PHARMA", "Pharmacie", null);
            TypeMetier publie    = brouillon.publier();

            when(depot.trouverParId(TYPE_ID)).thenReturn(Mono.just(brouillon));
            when(depot.sauvegarder(any())).thenReturn(Mono.just(publie));

            StepVerifier.create(service.publier(TYPE_ID, ctx))
                    .assertNext(t -> assertThat(t.statut()).isEqualTo(StatutType.PUBLIE))
                    .verifyComplete();
        }

        @Test
        @DisplayName("doit retourner 404 si le type est introuvable")
        void publier_404_si_introuvable() {
            when(depot.trouverParId(TYPE_ID)).thenReturn(Mono.empty());

            StepVerifier.create(service.publier(TYPE_ID, ctx))
                    .expectErrorMatches(e ->
                            e instanceof ProblemException pe &&
                            pe.getStatus().value() == 404)
                    .verify();
        }

        @Test
        @DisplayName("doit rejeter si le type appartient à un autre tenant")
        void publier_rejette_tenant_etranger() {
            TypeMetier autreType = TypeMetier.creer(UUID.randomUUID(), "PHARMA", "Pharmacie", null);

            when(depot.trouverParId(TYPE_ID)).thenReturn(Mono.just(autreType));

            StepVerifier.create(service.publier(TYPE_ID, ctx))
                    .expectErrorMatches(e ->
                            e instanceof ProblemException pe &&
                            pe.getStatus().value() == 403)
                    .verify();
        }
    }

    @Nested
    @DisplayName("archiver()")
    class Archiver {

        @Test
        @DisplayName("doit archiver un type PUBLIE")
        void archiver_ok() {
            TypeMetier publie  = TypeMetier.creer(TENANT_ID, "PHARMA", "Pharmacie", null).publier();
            TypeMetier archive = publie.archiver();

            when(depot.trouverParId(TYPE_ID)).thenReturn(Mono.just(publie));
            when(depot.sauvegarder(any())).thenReturn(Mono.just(archive));

            StepVerifier.create(service.archiver(TYPE_ID, ctx))
                    .assertNext(t -> assertThat(t.statut()).isEqualTo(StatutType.ARCHIVE))
                    .verifyComplete();
        }

        @Test
        @DisplayName("doit rejeter l'archivage d'un BROUILLON")
        void archiver_brouillon_leve_conflit() {
            TypeMetier brouillon = TypeMetier.creer(TENANT_ID, "PHARMA", "Pharmacie", null);

            when(depot.trouverParId(TYPE_ID)).thenReturn(Mono.just(brouillon));

            StepVerifier.create(service.archiver(TYPE_ID, ctx))
                    .expectErrorMatches(e -> e instanceof ProblemException)
                    .verify();
        }
    }
}
