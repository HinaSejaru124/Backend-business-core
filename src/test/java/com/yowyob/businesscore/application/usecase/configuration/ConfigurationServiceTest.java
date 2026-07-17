package com.yowyob.businesscore.application.usecase.configuration;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.configuration.ParametreConfig;
import com.yowyob.businesscore.domain.port.out.PersisterParametreConfig;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le use case de Configuration existait déjà (niveau TYPE et ENTREPRISE) mais n'avait encore aucun
 * appelant (aucune route REST n'exposait le niveau ENTREPRISE avant l'ajout de
 * {@code EntrepriseConfigController}) ni aucun test. Ces tests couvrent les deux niveaux et, surtout,
 * le verrou (RG absente de nom mais bien réelle : un paramètre {@code verrouille} au Type ne peut pas
 * être surchargé par une entreprise, 409 {@code PARAMETRE_VERROUILLE}).
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock PersisterParametreConfig depot;

    ConfigurationService service;

    private static final UUID TENANT = UUID.randomUUID();
    private final BusinessContext ctx = new BusinessContext(
            TENANT, null, Set.of(), null, "trace-test", Locale.FRENCH);

    private void init() {
        service = new ConfigurationService(depot);
    }

    @Test
    @DisplayName("definirPourType : premier paramètre → enregistré")
    void definirPourType_ok() {
        init();
        UUID versionTypeId = UUID.randomUUID();
        when(depot.trouverParCleEtVersion("devise", versionTypeId)).thenReturn(Mono.empty());
        when(depot.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.definirPourType(versionTypeId, "devise", "XAF", true, ctx))
                .assertNext(p -> {
                    assertThat(p.cle()).isEqualTo("devise");
                    assertThat(p.verrouille()).isTrue();
                    assertThat(p.estNiveauType()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("surchargerpourEntreprise : paramètre Type non verrouillé → surcharge acceptée")
    void surchargerPourEntreprise_ok_si_non_verrouille() {
        init();
        UUID versionTypeId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        ParametreConfig auTypeNonVerrouille = ParametreConfig.pourType(TENANT, versionTypeId, "tva", "19.25", false);
        when(depot.trouverParCleEtVersion("tva", versionTypeId)).thenReturn(Mono.just(auTypeNonVerrouille));
        when(depot.trouverParCleEtEntreprise("tva", entrepriseId)).thenReturn(Mono.empty());
        when(depot.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.surchargerpourEntreprise(entrepriseId, versionTypeId, "tva", "18", ctx))
                .assertNext(p -> {
                    assertThat(p.valeur()).isEqualTo("18");
                    assertThat(p.estNiveauEntreprise()).isTrue();
                    assertThat(p.verrouille()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("surchargerpourEntreprise : paramètre Type verrouillé → 409 PARAMETRE_VERROUILLE")
    void surchargerPourEntreprise_rejete_si_verrouille() {
        init();
        UUID versionTypeId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        ParametreConfig auTypeVerrouille = ParametreConfig.pourType(TENANT, versionTypeId, "devise", "XAF", true);
        when(depot.trouverParCleEtVersion("devise", versionTypeId)).thenReturn(Mono.just(auTypeVerrouille));

        StepVerifier.create(service.surchargerpourEntreprise(entrepriseId, versionTypeId, "devise", "EUR", ctx))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) e).getStatus().value()).isEqualTo(409);
                    assertThat(((ProblemException) e).getExtensions().get("violatedRule"))
                            .isEqualTo("PARAMETRE_VERROUILLE");
                })
                .verify();

        verify(depot, never()).sauvegarder(any());
    }

    @Test
    @DisplayName("surchargerpourEntreprise : aucun défaut Type déclaré → 404")
    void surchargerPourEntreprise_404_si_absent_au_type() {
        init();
        UUID versionTypeId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        when(depot.trouverParCleEtVersion("inconnu", versionTypeId)).thenReturn(Mono.empty());

        StepVerifier.create(service.surchargerpourEntreprise(entrepriseId, versionTypeId, "inconnu", "x", ctx))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) e).getStatus().value()).isEqualTo(404);
                })
                .verify();
    }
}
