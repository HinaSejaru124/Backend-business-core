package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.businesstype.VersionType;
import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.port.out.PersisterVersionType;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests de la déclaration d'opération : résolution de version, unicité du nom, persistance des étapes.
 */
@ExtendWith(MockitoExtension.class)
class DeclarerOperationServiceTest {

    @Mock PersisterOperation persisterOperation;
    @Mock PersisterVersionType persisterVersionType;

    DeclarerOperationService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID TYPE = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT, null, Set.of(), null, "trace-test", Locale.FRENCH);

    @BeforeEach
    void setUp() {
        service = new DeclarerOperationService(persisterOperation, persisterVersionType);
    }

    private List<EtapeDeclaration> etapesVente() {
        return List.of(
                new EtapeDeclaration(0, TypeEtape.VERIFIER_STOCK),
                new EtapeDeclaration(1, TypeEtape.EVALUER_REGLES),
                new EtapeDeclaration(2, TypeEtape.ENREGISTRER_VENTE));
    }

    @Test
    @DisplayName("déclare l'opération et ses étapes ordonnées")
    void declare_ok() {
        VersionType version = VersionType.creer(TYPE, TENANT, 1);
        when(persisterVersionType.trouverParTypeEtNumero(TYPE, 1)).thenReturn(Mono.just(version));
        when(persisterOperation.trouverParVersionEtNom(version.id(), "vente")).thenReturn(Mono.empty());
        when(persisterOperation.sauvegarderDefinition(any()))
                .thenAnswer(inv -> Mono.just((DefinitionOperation) inv.getArgument(0)));
        when(persisterOperation.sauvegarderEtapes(eq(TENANT), any()))
                .thenAnswer(inv -> Flux.fromIterable(inv.getArgument(1)));

        StepVerifier.create(service.declarer(TYPE, 1, "vente", "caissier",
                        Declencheur.AVANT_VENTE, false, etapesVente(), ctx))
                .assertNext(operation -> {
                    assertThat(operation.definition().nom()).isEqualTo("vente");
                    assertThat(operation.definition().tenantId()).isEqualTo(TENANT);
                    assertThat(operation.etapes()).hasSize(3);
                    assertThat(operation.etapes().get(0).typeEtape()).isEqualTo(TypeEtape.VERIFIER_STOCK);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("rejette un nom d'opération déjà pris pour la version (409)")
    void declare_conflit_nom() {
        VersionType version = VersionType.creer(TYPE, TENANT, 1);
        DefinitionOperation existante = DefinitionOperation.creer(
                TENANT, version.id(), "vente", null, Declencheur.AVANT_VENTE, false);
        when(persisterVersionType.trouverParTypeEtNumero(TYPE, 1)).thenReturn(Mono.just(version));
        when(persisterOperation.trouverParVersionEtNom(version.id(), "vente"))
                .thenReturn(Mono.just(existante));

        StepVerifier.create(service.declarer(TYPE, 1, "vente", null,
                        Declencheur.AVANT_VENTE, false, etapesVente(), ctx))
                .expectErrorMatches(e -> e instanceof ProblemException pe && pe.getStatus().value() == 409)
                .verify();
    }

    @Test
    @DisplayName("renvoie 404 si la version n'existe pas")
    void declare_version_introuvable() {
        when(persisterVersionType.trouverParTypeEtNumero(TYPE, 9)).thenReturn(Mono.empty());

        StepVerifier.create(service.declarer(TYPE, 9, "vente", null,
                        Declencheur.AVANT_VENTE, false, etapesVente(), ctx))
                .expectErrorMatches(e -> e instanceof ProblemException pe && pe.getStatus().value() == 404)
                .verify();
    }
}
