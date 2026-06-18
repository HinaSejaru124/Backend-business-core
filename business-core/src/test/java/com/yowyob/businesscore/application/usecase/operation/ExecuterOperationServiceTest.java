package com.yowyob.businesscore.application.usecase.operation;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.saga.MoteurOperation;
import com.yowyob.businesscore.application.saga.ResultatMoteur;
import com.yowyob.businesscore.domain.operation.DefinitionOperation;
import com.yowyob.businesscore.domain.operation.spi.EntrepriseResolue;
import com.yowyob.businesscore.domain.operation.spi.PersisterOperation;
import com.yowyob.businesscore.domain.operation.spi.ResoudreEntreprise;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.HorlogeSysteme;
import com.yowyob.businesscore.domain.port.internal.PlanificateurDOperation;
import com.yowyob.businesscore.domain.port.out.PublierEvenement;
import com.yowyob.businesscore.domain.port.out.VerrouDIdempotence;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.StatutTrace;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import com.yowyob.businesscore.domain.transaction.TraceOperation;
import com.yowyob.businesscore.domain.transaction.spi.PersisterTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du chef d'orchestre : idempotence, modes immédiat/différé, compensation.
 * Le {@link MoteurOperation} est mické pour isoler la logique d'orchestration de la trace et du code HTTP.
 */
@ExtendWith(MockitoExtension.class)
class ExecuterOperationServiceTest {

    @Mock ResoudreEntreprise resoudreEntreprise;
    @Mock PersisterOperation persisterOperation;
    @Mock PlanificateurDOperation planificateur;
    @Mock MoteurOperation moteur;
    @Mock PersisterTrace persisterTrace;
    @Mock VerrouDIdempotence verrou;
    @Mock PublierEvenement publierEvenement;
    @Mock HorlogeSysteme horloge;
    @Mock ObjectMapper objectMapper;

    ExecuterOperationService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ENTREPRISE = UUID.randomUUID();
    private static final UUID VERSION_TYPE = UUID.randomUUID();
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID OPERATION = UUID.randomUUID();
    private static final UUID TX_KERNEL = UUID.randomUUID();

    private final BusinessContext ctx = new BusinessContext(
            TENANT, UUID.randomUUID(), Set.of("caissier"), null, "trace-test", Locale.FRENCH);

    @BeforeEach
    void setUp() {
        service = new ExecuterOperationService(resoudreEntreprise, persisterOperation, planificateur,
                moteur, persisterTrace, verrou, publierEvenement, horloge, objectMapper);
    }

    private DefinitionOperation definition(boolean differe) {
        return new DefinitionOperation(OPERATION, TENANT, VERSION_TYPE, "vente",
                "caissier", Declencheur.AVANT_VENTE, differe);
    }

    private ContexteEtape contexteAvecVente() {
        return ContexteEtape.vide()
                .avec(ClesContexte.TRANSACTION_KERNEL_ID, TX_KERNEL)
                .avec(ClesContexte.MONTANT, new BigDecimal("1500"))
                .avec(ClesContexte.DEVISE, "XAF");
    }

    private void stubResolutionImmediate() {
        when(resoudreEntreprise.resoudre(ENTREPRISE))
                .thenReturn(Mono.just(new EntrepriseResolue(ENTREPRISE, VERSION_TYPE, ORG)));
        when(persisterOperation.trouverParVersionEtNom(VERSION_TYPE, "vente"))
                .thenReturn(Mono.just(definition(false)));
        when(planificateur.planifier(any(UUID.class)))
                .thenReturn(Flux.just(TypeEtape.ENREGISTRER_VENTE, TypeEtape.ENCAISSER));
        when(horloge.maintenant()).thenReturn(Instant.parse("2026-06-17T10:00:00Z"));
    }

    @Test
    @DisplayName("immédiat : succès → 200 COMPLETEE + trace + transaction kernel")
    void immediat_succes_completee() {
        when(persisterTrace.trouverParCleIdempotence(anyString())).thenReturn(Mono.empty());
        when(verrou.acquerir(anyString(), any())).thenReturn(Mono.just(true));
        when(verrou.liberer(anyString())).thenReturn(Mono.empty());
        stubResolutionImmediate();
        when(moteur.executer(any(), any()))
                .thenReturn(Mono.just(ResultatMoteur.succes(contexteAvecVente())));
        when(persisterTrace.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.executer(ENTREPRISE, "vente", "cle-1",
                        Map.of("offreId", UUID.randomUUID().toString(), "quantite", 2), ctx))
                .assertNext(resultat -> {
                    assertThat(resultat.statut()).isEqualTo(StatutTrace.COMPLETEE);
                    assertThat(resultat.transactionKernelId()).isEqualTo(TX_KERNEL);
                    assertThat(resultat.traceId()).isNotNull();
                    assertThat(resultat.details()).containsEntry(ClesContexte.DEVISE, "XAF");
                })
                .verifyComplete();

        ArgumentCaptor<TraceOperation> traceCaptor = ArgumentCaptor.forClass(TraceOperation.class);
        verify(persisterTrace).sauvegarder(traceCaptor.capture());
        assertThat(traceCaptor.getValue().statut()).isEqualTo(StatutTrace.COMPLETEE);
        verify(verrou).liberer(anyString());
    }

    @Test
    @DisplayName("règle bloquante : remonte 422, aucune trace persistée")
    void regle_bloquante_422_sans_trace() {
        when(persisterTrace.trouverParCleIdempotence(anyString())).thenReturn(Mono.empty());
        when(verrou.acquerir(anyString(), any())).thenReturn(Mono.just(true));
        when(verrou.liberer(anyString())).thenReturn(Mono.empty());
        stubResolutionImmediate();
        ProblemException blocage = ProblemException.unprocessable("Document requis").requiredAction("EXIGER");
        when(moteur.executer(any(), any()))
                .thenReturn(Mono.just(ResultatMoteur.echec(ContexteEtape.vide(), blocage)));

        StepVerifier.create(service.executer(ENTREPRISE, "vente", "cle-2", Map.of(), ctx))
                .expectErrorMatches(e -> e instanceof ProblemException pe && pe.getStatus().value() == 422)
                .verify();

        verify(persisterTrace, never()).sauvegarder(any());
        verify(verrou).liberer(anyString());
    }

    @Test
    @DisplayName("échec après vente : compensation → trace COMPENSEE + erreur propagée")
    void echec_apres_vente_compensee() {
        when(persisterTrace.trouverParCleIdempotence(anyString())).thenReturn(Mono.empty());
        when(verrou.acquerir(anyString(), any())).thenReturn(Mono.just(true));
        when(verrou.liberer(anyString())).thenReturn(Mono.empty());
        stubResolutionImmediate();
        RuntimeException panne = new RuntimeException("encaissement kernel indisponible");
        when(moteur.executer(any(), any()))
                .thenReturn(Mono.just(ResultatMoteur.echec(contexteAvecVente(), panne)));
        when(persisterTrace.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.executer(ENTREPRISE, "vente", "cle-3", Map.of(), ctx))
                .expectErrorMatches(e -> e == panne)
                .verify();

        ArgumentCaptor<TraceOperation> traceCaptor = ArgumentCaptor.forClass(TraceOperation.class);
        verify(persisterTrace).sauvegarder(traceCaptor.capture());
        assertThat(traceCaptor.getValue().statut()).isEqualTo(StatutTrace.COMPENSEE);
        assertThat(traceCaptor.getValue().transactionKernelId()).isEqualTo(TX_KERNEL);
    }

    @Test
    @DisplayName("idempotence : rejouer la même clé renvoie la trace existante sans réexécuter")
    void idempotence_rejeu_sans_doublon() {
        TraceOperation existante = TraceOperation.demarrer(TENANT, ENTREPRISE, OPERATION, "vente",
                        "cle-rejeu", Instant.parse("2026-06-17T09:00:00Z"))
                .completer(TX_KERNEL, null, Instant.parse("2026-06-17T09:00:01Z"));
        when(persisterTrace.trouverParCleIdempotence("cle-rejeu")).thenReturn(Mono.just(existante));

        StepVerifier.create(service.executer(ENTREPRISE, "vente", "cle-rejeu", Map.of(), ctx))
                .assertNext(resultat -> {
                    assertThat(resultat.statut()).isEqualTo(StatutTrace.COMPLETEE);
                    assertThat(resultat.transactionKernelId()).isEqualTo(TX_KERNEL);
                })
                .verifyComplete();

        verify(moteur, never()).executer(any(), any());
        verify(verrou, never()).acquerir(anyString(), any());
    }

    @Test
    @DisplayName("différé : renvoie 202 EN_COURS + trace + événement publié, sans exécuter le moteur")
    void differe_202_en_cours() {
        when(persisterTrace.trouverParCleIdempotence(anyString())).thenReturn(Mono.empty());
        when(verrou.acquerir(anyString(), any())).thenReturn(Mono.just(true));
        when(verrou.liberer(anyString())).thenReturn(Mono.empty());
        when(resoudreEntreprise.resoudre(ENTREPRISE))
                .thenReturn(Mono.just(new EntrepriseResolue(ENTREPRISE, VERSION_TYPE, ORG)));
        when(persisterOperation.trouverParVersionEtNom(VERSION_TYPE, "commande"))
                .thenReturn(Mono.just(new DefinitionOperation(OPERATION, TENANT, VERSION_TYPE, "commande",
                        null, Declencheur.AVANT_OPERATION, true)));
        when(horloge.maintenant()).thenReturn(Instant.parse("2026-06-17T10:00:00Z"));
        when(persisterTrace.sauvegarder(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(publierEvenement.publier(anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.executer(ENTREPRISE, "commande", "cle-4", Map.of(), ctx))
                .assertNext(resultat -> {
                    assertThat(resultat.statut()).isEqualTo(StatutTrace.EN_COURS);
                    assertThat(resultat.traceId()).isNotNull();
                })
                .verifyComplete();

        verify(moteur, never()).executer(any(), any());
        verify(publierEvenement).publier(anyString(), any());
    }
}
