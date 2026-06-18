package com.yowyob.businesscore.application.offer;

import com.yowyob.businesscore.application.capacite.RegistreCapacites;
import com.yowyob.businesscore.application.usecase.offer.GestionOffreService;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.in.offer.GestionOffre.DeclarerOffreCommande;
import com.yowyob.businesscore.domain.port.internal.offer.FournisseurDeCapacite;
import com.yowyob.businesscore.domain.port.out.offer.DepotOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import com.yowyob.businesscore.shared.error.ProblemException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionOffreServiceTest {

    private final DepotOffre depot = mock(DepotOffre.class);
    private final RegistreCapacites registre = mock(RegistreCapacites.class);
    private final GestionOffreService service = new GestionOffreService(depot, registre);

    @Test
    void declarer_offre_stockable_active_la_strategie_correspondante() {
        when(depot.enregistrer(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        FournisseurDeCapacite stockable = mock(FournisseurDeCapacite.class);
        when(stockable.activer(any())).thenReturn(Mono.empty());
        when(registre.pour(TypeCapacite.STOCKABLE)).thenReturn(Optional.of(stockable));

        DeclarerOffreCommande cmd = new DeclarerOffreCommande(
                UUID.randomUUID(), "Bouteille de gaz", FormePrix.FIXE,
                new BigDecimal("5000"), Set.of(TypeCapacite.STOCKABLE));

        StepVerifier.create(service.declarer(cmd))
                .assertNext(offre -> {
                    assert offre.formePrix() == FormePrix.FIXE;
                    assert offre.possede(TypeCapacite.STOCKABLE);
                })
                .verifyComplete();

        verify(stockable, times(1)).activer(any(DefinitionOffre.class));
    }

    @Test
    void declarer_offre_avec_capacite_non_supportee_echoue_en_422() {
        when(depot.enregistrer(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(registre.pour(TypeCapacite.PLANIFIABLE)).thenReturn(Optional.empty());

        DeclarerOffreCommande cmd = new DeclarerOffreCommande(
                UUID.randomUUID(), "Consultation", FormePrix.GRATUIT,
                null, Set.of(TypeCapacite.PLANIFIABLE));

        StepVerifier.create(service.declarer(cmd))
                .expectErrorSatisfies(e -> {
                    assert e instanceof ProblemException;
                })
                .verify();
    }

    @Test
    void offre_fixe_sans_prix_est_rejetee_par_invariant_domaine() {
        when(depot.enregistrer(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        DeclarerOffreCommande cmd = new DeclarerOffreCommande(
                UUID.randomUUID(), "Offre cassée", FormePrix.FIXE, null, Set.of());

        StepVerifier.create(service.declarer(cmd))
                .expectError(ProblemException.class)
                .verify();
    }
}
