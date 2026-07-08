package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.FournisseurDeCapaciteDispatcher;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotProduitEntreprise;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GestionOffreServiceTest {

    private final DepotOffre depot = mock(DepotOffre.class);
    private final DepotProduitEntreprise depotProduit = mock(DepotProduitEntreprise.class);
    private final FournisseurDeCapaciteDispatcher capacites = mock(FournisseurDeCapaciteDispatcher.class);
    private final GestionOffreService service = new GestionOffreService(depot, depotProduit, capacites);

    @Test
    void declarer_offre_stockable_active_la_strategie_correspondante() {
        when(depot.enregistrer(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(capacites.activer(eq(TypeCapacite.STOCKABLE), any(UUID.class))).thenReturn(Mono.empty());

        GestionOffreService.DeclarerOffreCommande cmd = new GestionOffreService.DeclarerOffreCommande(
                UUID.randomUUID(), "Bouteille de gaz", FormePrix.FIXE,
                new BigDecimal("5000"), Set.of(TypeCapacite.STOCKABLE));

        StepVerifier.create(service.declarer(cmd))
                .assertNext(offre -> {
                    assert offre.formePrix() == FormePrix.FIXE;
                    assert offre.possede(TypeCapacite.STOCKABLE);
                })
                .verifyComplete();

        verify(capacites, times(1)).activer(eq(TypeCapacite.STOCKABLE), any(UUID.class));
    }

    @Test
    void declarer_offre_avec_capacite_non_supportee_echoue_en_422() {
        when(depot.enregistrer(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(capacites.activer(eq(TypeCapacite.PLANIFIABLE), any(UUID.class)))
                .thenReturn(Mono.error(ProblemException.unprocessable("Capacité non encore supportée : PLANIFIABLE")));

        GestionOffreService.DeclarerOffreCommande cmd = new GestionOffreService.DeclarerOffreCommande(
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
        GestionOffreService.DeclarerOffreCommande cmd = new GestionOffreService.DeclarerOffreCommande(
                UUID.randomUUID(), "Offre cassée", FormePrix.FIXE, null, Set.of());

        StepVerifier.create(service.declarer(cmd))
                .expectError(ProblemException.class)
                .verify();
    }
}
