// test/.../EvaluateurDeRegleN1Test.java
package com.yowyob.businesscore.application.usecase.rule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.yowyob.businesscore.domain.port.internal.ContexteEvaluation;
import com.yowyob.businesscore.domain.port.internal.EffetAAppliquer;
import com.yowyob.businesscore.domain.port.out.RegistreDeRegles;
import com.yowyob.businesscore.domain.port.out.RegleChargee;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class EvaluateurDeRegleN1Test {

    private RegistreDeRegles registre;
    private EvaluateurDeRegleN1 evaluateur;

    @BeforeEach
    void setup() {
        registre = Mockito.mock(RegistreDeRegles.class);
        evaluateur = new EvaluateurDeRegleN1(registre, new EvaluateurConditionN1());
    }

    private RegleChargee regle(Effet effet, String condition) {
        return new RegleChargee(UUID.randomUUID(), Declencheur.AVANT_VENTE,
                condition, effet, List.of(), true);
    }

    private ContexteEvaluation ctx(Map<String, Object> valeurs) {
        return new ContexteEvaluation(valeurs);
    }

    @Test
    void effet_BLOQUER_condition_toujours_vrai() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.BLOQUER, "TOUJOURS_VRAI")));

        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE, ctx(Map.of())))
                .assertNext(e -> assertThat(e.effet()).isEqualTo(Effet.BLOQUER))
                .verifyComplete();
    }

    @Test
    void effet_EXIGER_condition_categorie_egale_satisfaite() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.EXIGER,
                        "CATEGORIE_EGALE:valeur=medicament_prescription")));

        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE,
                        ctx(Map.of("categorie", "medicament_prescription"))))
                .assertNext(e -> assertThat(e.effet()).isEqualTo(Effet.EXIGER))
                .verifyComplete();
    }

    @Test
    void condition_non_satisfaite_ne_produit_aucun_effet() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.BLOQUER, "MONTANT_SUPERIEUR:seuil=1000")));

        // montant 500 < seuil 1000 → condition non satisfaite → Flux vide
        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE,
                        ctx(Map.of("montant", 500))))
                .verifyComplete();
    }

    @Test
    void effet_ALERTER_passe_sans_bloquer() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.ALERTER, "TOUJOURS_VRAI")));

        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE, ctx(Map.of())))
                .assertNext(e -> assertThat(e.effet()).isEqualTo(Effet.ALERTER))
                .verifyComplete();
    }

    @Test
    void effet_VALIDER_condition_satisfaite() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.VALIDER, "TOUJOURS_VRAI")));

        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE, ctx(Map.of())))
                .assertNext(e -> assertThat(e.effet()).isEqualTo(Effet.VALIDER))
                .verifyComplete();
    }

    @Test
    void effet_DEROGER_liste_vide_roles_confirme_en_domaine() {
        RegleChargee r = new RegleChargee(
                UUID.randomUUID(), Declencheur.AVANT_VENTE,
                "TOUJOURS_VRAI", Effet.DEROGER,
                List.of(), // liste vide → personne autorisé
                true);

        assertThat(r.rolesAutorisesADeroger()).isEmpty();
    }

    @Test
    void effet_AJUSTER_details_contiennent_ancienne_valeur() {
        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regle(Effet.AJUSTER, "TOUJOURS_VRAI")));

        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE,
                        ctx(Map.of("valeurCible", "valeurOriginale"))))
                .assertNext((EffetAAppliquer e) -> {
                    assertThat(e.effet()).isEqualTo(Effet.AJUSTER);
                    assertThat(e.details()).containsKey("ancienneValeur");
                })
                .verifyComplete();
    }

    @Test
    void plusieurs_regles_dont_une_non_satisfaite_ne_retourne_que_les_satisfaites() {
        RegleChargee regleSatisfaite = regle(Effet.ALERTER, "TOUJOURS_VRAI");
        RegleChargee regleNonSatisfaite = regle(Effet.BLOQUER, "MONTANT_SUPERIEUR:seuil=9999");

        when(registre.chargerPourDeclencheur(any(), any()))
                .thenReturn(Flux.just(regleSatisfaite, regleNonSatisfaite));

        // montant 100 < 9999 → seule ALERTER passe
        StepVerifier.create(evaluateur.evaluer(Declencheur.AVANT_VENTE,
                        ctx(Map.of("montant", 100))))
                .assertNext(e -> assertThat(e.effet()).isEqualTo(Effet.ALERTER))
                .verifyComplete();
    }
}