package com.yowyob.businesscore.application.usecase.rule;

import com.yowyob.businesscore.adapter.out.persistence.businesstype.VersionTypeEntity;
import com.yowyob.businesscore.adapter.out.persistence.businesstype.VersionTypeRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.enterprise.spi.LireEntreprise;
import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import com.yowyob.businesscore.domain.rule.RegleMetier;
import com.yowyob.businesscore.domain.rule.spi.DepotRegle;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RG-03 : une version publiée ne peut plus recevoir/modifier/perdre de règle de Type. */
@ExtendWith(MockitoExtension.class)
class GestionRegleServiceTest {

    @Mock DepotRegle depot;
    @Mock VersionTypeRepository versionTypeRepo;
    @Mock LireEntreprise lireEntreprise;
    @Mock JournaliserChangementSync journaliserSync;

    private GestionRegleService service;

    private static final UUID TYPE = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT, null, Set.of(), null, "trace-test", Locale.FRENCH);

    private void init() {
        service = new GestionRegleService(depot, versionTypeRepo, lireEntreprise, journaliserSync);
    }

    private VersionTypeEntity versionBrouillon() {
        return VersionTypeEntity.nouveau(UUID.randomUUID(), TENANT, TYPE, 1, false, null);
    }

    private VersionTypeEntity versionPubliee() {
        return VersionTypeEntity.nouveau(UUID.randomUUID(), TENANT, TYPE, 1, true, Instant.now());
    }

    @Test
    @DisplayName("creerRegleDeType : version en brouillon → règle créée")
    void creerRegleDeType_ok_sur_version_brouillon() {
        init();
        VersionTypeEntity version = versionBrouillon();
        when(versionTypeRepo.findByTypeMetierIdAndNumero(TYPE, 1)).thenReturn(Mono.just(version));
        when(depot.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.creerRegleDeType(
                        TYPE, 1, Declencheur.AVANT_VENTE, "TOUJOURS_VRAI", Effet.ALERTER, List.of(), ctx))
                .assertNext(regle -> assertThat(regle.getVersionTypeId()).isEqualTo(version.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("creerRegleDeType : version publiée → 409 RG-03, aucune règle enregistrée")
    void creerRegleDeType_sur_version_publiee_est_rejete_RG03() {
        init();
        when(versionTypeRepo.findByTypeMetierIdAndNumero(TYPE, 1)).thenReturn(Mono.just(versionPubliee()));

        StepVerifier.create(service.creerRegleDeType(
                        TYPE, 1, Declencheur.AVANT_VENTE, "TOUJOURS_VRAI", Effet.ALERTER, List.of(), ctx))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) e).getStatus().value()).isEqualTo(409);
                    assertThat(((ProblemException) e).getExtensions().get("violatedRule")).isEqualTo("RG-03");
                })
                .verify();

        verify(depot, never()).sauvegarder(any());
    }

    @Test
    @DisplayName("modifierDeType : version publiée → 409 RG-03, règle jamais rechargée ni resauvegardée")
    void modifierDeType_sur_version_publiee_est_rejete_RG03() {
        init();
        when(versionTypeRepo.findByTypeMetierIdAndNumero(TYPE, 1)).thenReturn(Mono.just(versionPubliee()));

        StepVerifier.create(service.modifierDeType(
                        TYPE, 1, UUID.randomUUID(), Declencheur.AVANT_VENTE, "TOUJOURS_VRAI",
                        Effet.ALERTER, List.of()))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) e).getExtensions().get("violatedRule")).isEqualTo("RG-03");
                })
                .verify();

        verify(depot, never()).trouverParId(any());
        verify(depot, never()).sauvegarder(any());
    }

    @Test
    @DisplayName("supprimerDeType : version publiée → 409 RG-03, aucune suppression")
    void supprimerDeType_sur_version_publiee_est_rejete_RG03() {
        init();
        when(versionTypeRepo.findByTypeMetierIdAndNumero(TYPE, 1)).thenReturn(Mono.just(versionPubliee()));

        StepVerifier.create(service.supprimerDeType(TYPE, 1, UUID.randomUUID()))
                .expectErrorSatisfies(e -> {
                    assertThat(e).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) e).getExtensions().get("violatedRule")).isEqualTo("RG-03");
                })
                .verify();

        verify(depot, never()).supprimer(any());
    }

    @Test
    @DisplayName("creerRegleLocale : jamais soumise à RG-03 (aucune version impliquée)")
    void creerRegleLocale_non_soumise_a_RG03() {
        init();
        UUID entrepriseId = UUID.randomUUID();
        com.yowyob.businesscore.domain.enterprise.Entreprise entreprise =
                new com.yowyob.businesscore.domain.enterprise.Entreprise(
                        entrepriseId, TENANT, UUID.randomUUID(), UUID.randomUUID(),
                        1, UUID.randomUUID(), null, null, "Pharma Test",
                        com.yowyob.businesscore.domain.shared.CycleVie.ACTIVE);
        when(lireEntreprise.parId(entrepriseId)).thenReturn(Mono.just(entreprise));
        when(depot.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(journaliserSync.journaliser(any(), any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.creerRegleLocale(
                        entrepriseId, Declencheur.AVANT_VENTE, "TOUJOURS_VRAI", Effet.ALERTER, List.of(), ctx))
                .assertNext(regle -> assertThat(regle.getEntrepriseId()).isEqualTo(entrepriseId))
                .verifyComplete();

        verify(versionTypeRepo, never()).findByTypeMetierIdAndNumero(any(), anyInt());
    }
}
