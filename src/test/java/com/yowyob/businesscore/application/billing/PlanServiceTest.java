package com.yowyob.businesscore.application.billing;

import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestEntity;
import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.port.out.PaiementPort;
import com.yowyob.businesscore.domain.port.out.PaiementPort.ResultatPaiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link PlanService} : exigence du numéro payeur, ouverture d'un paiement asynchrone
 * (EN_ATTENTE sans activation) et finalisation (activation uniquement sur CONFIRME).
 */
class PlanServiceTest {

    private DeveloperAccountRepository developerRepository;
    private PlanChangeRequestRepository changeRepository;
    private PlanCatalogue catalogue;
    private PaiementPort paiement;
    private PlanService service;

    private final UUID devId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        developerRepository = mock(DeveloperAccountRepository.class);
        changeRepository = mock(PlanChangeRequestRepository.class);
        catalogue = mock(PlanCatalogue.class);
        paiement = mock(PaiementPort.class);
        service = new PlanService(developerRepository, changeRepository, catalogue, paiement);

        when(catalogue.existe("PRO")).thenReturn(true);
        when(catalogue.normaliser("PRO")).thenReturn("PRO");
        when(catalogue.normaliser("FREE")).thenReturn("FREE");
        when(catalogue.prixMensuel("PRO")).thenReturn(15000L);
        when(catalogue.devise("PRO")).thenReturn("XAF");
    }

    private DeveloperAccountEntity compte(String plan) {
        return DeveloperAccountEntity.nouveau(devId, "dev@test.io", UUID.randomUUID(), "kuid", "kcid", "sec", plan);
    }

    private PlanChangeRequestEntity demandeEnAttente() {
        return PlanChangeRequestEntity.nouveau(devId, "FREE", "PRO", "EN_ATTENTE", "order-1");
    }

    @Test
    @DisplayName("changer exige le numéro mobile money du payeur")
    void changer_exige_payer_reference() {
        StepVerifier.create(service.changer(devId, "PRO", "  "))
                .expectErrorMatches(e -> e instanceof ProblemException pe
                        && "PAYER_REFERENCE_MANQUANT".equals(pe.getExtensions().get("violatedRule")))
                .verify();
    }

    @Test
    @DisplayName("changer ouvre un paiement EN_ATTENTE sans activer le plan")
    void changer_ouvre_paiement_en_attente() {
        when(developerRepository.findById(devId)).thenReturn(Mono.just(compte("FREE")));
        when(paiement.demanderPaiement(any())).thenReturn(Mono.just(
                new ResultatPaiement(ResultatPaiement.Statut.EN_ATTENTE, "https://pay", "order-1")));
        when(changeRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.changer(devId, "PRO", "692162333"))
                .assertNext(r -> {
                    assertThat(r.statut()).isEqualTo("EN_ATTENTE");
                    assertThat(r.plan()).isEqualTo("FREE"); // plan inchangé tant que non finalisé
                    assertThat(r.urlPaiement()).isEqualTo("https://pay");
                })
                .verifyComplete();

        verify(developerRepository, never()).save(any());
    }

    @Test
    @DisplayName("finaliser active le plan quand le paiement est CONFIRME")
    void finaliser_confirme_active_le_plan() {
        DeveloperAccountEntity compte = compte("FREE");
        when(changeRepository.findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(devId, "EN_ATTENTE"))
                .thenReturn(Mono.just(demandeEnAttente()));
        when(paiement.verifierStatut("order-1")).thenReturn(Mono.just(
                new ResultatPaiement(ResultatPaiement.Statut.CONFIRME, null, "order-1")));
        when(developerRepository.findById(devId)).thenReturn(Mono.just(compte));
        when(developerRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(changeRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.finaliser(devId))
                .assertNext(r -> {
                    assertThat(r.statut()).isEqualTo("CONFIRME");
                    assertThat(r.plan()).isEqualTo("PRO");
                })
                .verifyComplete();

        assertThat(compte.getPlan()).isEqualTo("PRO");
        verify(developerRepository).save(any());
    }

    @Test
    @DisplayName("finaliser n'active rien tant que le paiement est EN_ATTENTE")
    void finaliser_en_attente_n_active_rien() {
        when(changeRepository.findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(devId, "EN_ATTENTE"))
                .thenReturn(Mono.just(demandeEnAttente()));
        when(paiement.verifierStatut("order-1")).thenReturn(Mono.just(
                new ResultatPaiement(ResultatPaiement.Statut.EN_ATTENTE, "https://pay", "order-1")));

        StepVerifier.create(service.finaliser(devId))
                .assertNext(r -> assertThat(r.statut()).isEqualTo("EN_ATTENTE"))
                .verifyComplete();

        verify(developerRepository, never()).save(any());
    }

    @Test
    @DisplayName("finaliser propage un refus de paiement")
    void finaliser_refuse() {
        when(changeRepository.findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(devId, "EN_ATTENTE"))
                .thenReturn(Mono.just(demandeEnAttente()));
        when(paiement.verifierStatut("order-1")).thenReturn(Mono.just(
                new ResultatPaiement(ResultatPaiement.Statut.REFUSE, null, "order-1")));
        when(changeRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.finaliser(devId))
                .expectErrorMatches(e -> e instanceof ProblemException pe
                        && "PAIEMENT_REFUSE".equals(pe.getExtensions().get("violatedRule")))
                .verify();

        verify(developerRepository, never()).save(any());
    }

    @Test
    @DisplayName("finaliser sans paiement en attente renvoie une erreur 404")
    void finaliser_aucun_paiement() {
        when(changeRepository.findFirstByDeveloperIdAndStatutOrderByCreatedAtDesc(devId, "EN_ATTENTE"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.finaliser(devId))
                .expectErrorMatches(e -> e instanceof ProblemException pe
                        && "AUCUN_PAIEMENT_EN_ATTENTE".equals(pe.getExtensions().get("violatedRule")))
                .verify();
    }
}
