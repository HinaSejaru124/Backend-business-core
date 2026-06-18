package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la saga orchestrée locale : chaînage des étapes et compensation en ordre inverse (LIFO)
 * des étapes déjà réussies en cas d'échec. La compensation est déléguée au dispatcher (no-op pour les
 * étapes non compensables) — ici le dispatcher est mocké.
 */
@ExtendWith(MockitoExtension.class)
class MoteurOperationTest {

    @Mock ExecuteurDEtapeDispatcher dispatcher;

    MoteurOperation moteur;

    private static final UUID COMMANDE = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        moteur = new MoteurOperation(dispatcher);
    }

    @Test
    @DisplayName("toutes les étapes réussissent → succès, aucune compensation")
    void chaine_complete_sans_compensation() {
        when(dispatcher.executer(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenAnswer(inv -> Mono.just(((ContexteEtape) inv.getArgument(1))
                        .avec(ClesContexte.COMMANDE_ID, COMMANDE)));
        when(dispatcher.executer(eq(TypeEtape.ENCAISSER), any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        StepVerifier.create(moteur.executer(
                        List.of(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER), ContexteEtape.vide()))
                .assertNext(resultat -> {
                    assertThat(resultat.succes()).isTrue();
                    assertThat(resultat.contexte().get(ClesContexte.COMMANDE_ID)).isEqualTo(COMMANDE);
                })
                .verifyComplete();

        verify(dispatcher, never()).compenser(any(), any());
    }

    @Test
    @DisplayName("échec après un effet engagé → compense la vente, pas l'étape échouée")
    void echec_apres_effet_compense_en_ordre_inverse() {
        when(dispatcher.executer(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenAnswer(inv -> Mono.just(((ContexteEtape) inv.getArgument(1))
                        .avec(ClesContexte.COMMANDE_ID, COMMANDE)));
        when(dispatcher.executer(eq(TypeEtape.ENCAISSER), any()))
                .thenReturn(Mono.error(new RuntimeException("cashier KO")));
        when(dispatcher.compenser(eq(TypeEtape.ENREGISTRER_VENTE), any())).thenReturn(Mono.empty());

        StepVerifier.create(moteur.executer(
                        List.of(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER), ContexteEtape.vide()))
                .assertNext(resultat -> {
                    assertThat(resultat.succes()).isFalse();
                    assertThat(resultat.erreur()).hasMessage("cashier KO");
                })
                .verifyComplete();

        // L'étape réussie est compensée ; l'étape qui a échoué n'a rien engagé → pas de compensation.
        verify(dispatcher).compenser(eq(TypeEtape.ENREGISTRER_VENTE), any());
        verify(dispatcher, never()).compenser(eq(TypeEtape.ENCAISSER), any());
    }

    @Test
    @DisplayName("compensation best-effort : une annulation en échec ne masque pas l'erreur d'origine")
    void compensation_best_effort() {
        when(dispatcher.executer(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenAnswer(inv -> Mono.just(((ContexteEtape) inv.getArgument(1))
                        .avec(ClesContexte.COMMANDE_ID, COMMANDE)));
        when(dispatcher.executer(eq(TypeEtape.ENCAISSER), any()))
                .thenReturn(Mono.error(new RuntimeException("cashier KO")));
        when(dispatcher.compenser(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenReturn(Mono.error(new RuntimeException("cancel KO")));

        StepVerifier.create(moteur.executer(
                        List.of(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER), ContexteEtape.vide()))
                .assertNext(resultat -> {
                    assertThat(resultat.succes()).isFalse();
                    assertThat(resultat.erreur()).hasMessage("cashier KO");
                })
                .verifyComplete();

        verify(dispatcher).compenser(eq(TypeEtape.ENREGISTRER_VENTE), any());
    }

    @Test
    @DisplayName("échec avant tout effet → aucune compensation")
    void echec_avant_effet_sans_compensation() {
        when(dispatcher.executer(eq(TypeEtape.VERIFIER_STOCK), any()))
                .thenReturn(Mono.error(new RuntimeException("stock indispo")));

        StepVerifier.create(moteur.executer(List.of(TypeEtape.VERIFIER_STOCK), ContexteEtape.vide()))
                .assertNext(resultat -> assertThat(resultat.succes()).isFalse())
                .verifyComplete();

        verify(dispatcher, never()).compenser(any(), any());
    }
}
