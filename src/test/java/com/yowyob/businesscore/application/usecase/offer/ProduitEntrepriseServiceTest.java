package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.domain.offer.Capacite;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotProduitEntreprise;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.out.CreationProduit;
import com.yowyob.businesscore.domain.port.out.GererCatalogueOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Vérifie la résolution paresseuse du produit kernel par entreprise : mapping existant → réutilisé sans
 * création ; mapping absent → création kernel (champs dérivés) + mémorisation du mapping.
 */
class ProduitEntrepriseServiceTest {

    private final DepotProduitEntreprise depotProduit = mock(DepotProduitEntreprise.class);
    private final DepotOffre depotOffre = mock(DepotOffre.class);
    private final ResolveurContexteKernel resolveur = mock(ResolveurContexteKernel.class);
    private final GererCatalogueOffre catalogue = mock(GererCatalogueOffre.class);
    private final ProduitEntrepriseService service =
            new ProduitEntrepriseService(depotProduit, depotOffre, resolveur, catalogue);

    @Test
    @DisplayName("mapping existant : renvoie le productId mémorisé sans créer de produit kernel")
    void mapping_existant_pas_de_creation() {
        UUID businessId = UUID.randomUUID();
        UUID offreId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(depotProduit.trouverProductId(businessId, offreId)).thenReturn(Mono.just(productId));

        StepVerifier.create(service.resoudre(businessId, offreId))
                .expectNext(productId)
                .verifyComplete();

        verify(catalogue, never()).creerProduit(any());
        verify(depotProduit, never()).enregistrer(any(), any(), any());
    }

    @Test
    @DisplayName("mapping absent : crée le produit (champs dérivés) puis mémorise le mapping")
    void mapping_absent_cree_et_memorise() {
        UUID businessId = UUID.randomUUID();
        UUID offreId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        DefinitionOffre offre = DefinitionOffre
                .nouvelle(offreId, UUID.randomUUID(), "Bouteille de gaz", FormePrix.FIXE, new BigDecimal("5000"))
                .avecCapacites(List.of(Capacite.nouvelle(
                        UUID.randomUUID(), offreId, TypeCapacite.STOCKABLE, true)));

        when(depotProduit.trouverProductId(businessId, offreId)).thenReturn(Mono.empty());
        when(depotOffre.parId(offreId)).thenReturn(Mono.just(offre));
        when(resolveur.resoudre(businessId)).thenReturn(Mono.just(new ContexteKernel(
                organizationId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "XAF")));
        when(catalogue.creerProduit(any())).thenReturn(Mono.just(productId));
        when(depotProduit.enregistrer(eq(businessId), eq(offreId), eq(productId))).thenReturn(Mono.empty());

        StepVerifier.create(service.resoudre(businessId, offreId))
                .expectNext(productId)
                .verifyComplete();

        ArgumentCaptor<CreationProduit> captor = ArgumentCaptor.forClass(CreationProduit.class);
        verify(catalogue).creerProduit(captor.capture());
        CreationProduit demande = captor.getValue();
        assertThat(demande.organizationId()).isEqualTo(organizationId);
        assertThat(demande.nom()).isEqualTo("Bouteille de gaz");
        assertThat(demande.sku()).startsWith("OFFRE-").contains("GAZ");
        assertThat(demande.familyCode()).isEqualTo("GENERAL");
        assertThat(demande.variantLabel()).isEqualTo("STANDARD");
        assertThat(demande.unitPrice()).isEqualByComparingTo("5000");
        assertThat(demande.currency()).isEqualTo("XAF");
        assertThat(demande.stockable()).isTrue();
        assertThat(demande.priceless()).isFalse();

        verify(depotProduit).enregistrer(businessId, offreId, productId);
    }
}
