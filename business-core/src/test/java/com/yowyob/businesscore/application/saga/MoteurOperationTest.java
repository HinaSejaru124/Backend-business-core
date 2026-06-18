package com.yowyob.businesscore.application.saga;

import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.out.ExecuterWorkflow;
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
 * Tests du moteur saga local : chaînage des étapes et compensation de l'effet engagé en cas d'échec.
 */
@ExtendWith(MockitoExtension.class)
class MoteurOperationTest {

    @Mock ExecuteurDEtapeDispatcher dispatcher;
    @Mock ExecuterWorkflow executerWorkflow;

    MoteurOperation moteur;

    private static final UUID TX = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        moteur = new MoteurOperation(dispatcher, executerWorkflow);
    }

    @Test
    @DisplayName("toutes les étapes réussissent → succès, pas de compensation")
    void chaine_complete_sans_compensation() {
        when(dispatcher.executer(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenAnswer(inv -> Mono.just(((ContexteEtape) inv.getArgument(1))
                        .avec(ClesContexte.TRANSACTION_KERNEL_ID, TX)));
        when(dispatcher.executer(eq(TypeEtape.ENCAISSER), any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(1)));

        StepVerifier.create(moteur.executer(
                        List.of(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER), ContexteEtape.vide()))
                .assertNext(resultat -> {
                    assertThat(resultat.succes()).isTrue();
                    assertThat(resultat.contexte().get(ClesContexte.TRANSACTION_KERNEL_ID)).isEqualTo(TX);
                })
                .verifyComplete();

        verify(executerWorkflow, never()).compenser(any());
    }

    @Test
    @DisplayName("échec après un effet engagé → compense la transaction kernel")
    void echec_apres_effet_compense() {
        when(dispatcher.executer(eq(TypeEtape.ENREGISTRER_VENTE), any()))
                .thenAnswer(inv -> Mono.just(((ContexteEtape) inv.getArgument(1))
                        .avec(ClesContexte.TRANSACTION_KERNEL_ID, TX)));
        when(dispatcher.executer(eq(TypeEtape.ENCAISSER), any()))
                .thenReturn(Mono.error(new RuntimeException("cashier KO")));
        when(executerWorkflow.compenser(TX.toString())).thenReturn(Mono.empty());

        StepVerifier.create(moteur.executer(
                        List.of(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER), ContexteEtape.vide()))
                .assertNext(resultat -> {
                    assertThat(resultat.succes()).isFalse();
                    assertThat(resultat.erreur()).hasMessage("cashier KO");
                })
                .verifyComplete();

        verify(executerWorkflow).compenser(TX.toString());
    }

    @Test
    @DisplayName("échec avant tout effet → aucune compensation")
    void echec_avant_effet_sans_compensation() {
        when(dispatcher.executer(eq(TypeEtape.VERIFIER_STOCK), any()))
                .thenReturn(Mono.error(new RuntimeException("stock indispo")));

        StepVerifier.create(moteur.executer(List.of(TypeEtape.VERIFIER_STOCK), ContexteEtape.vide()))
                .assertNext(resultat -> assertThat(resultat.succes()).isFalse())
                .verifyComplete();

        verify(executerWorkflow, never()).compenser(any());
    }
}
