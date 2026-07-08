package com.yowyob.businesscore.application.usecase.offer;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotOffre;
import com.yowyob.businesscore.domain.offer.spi.DepotProduitEntreprise;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.internal.ResoudreProduitEntreprise;
import com.yowyob.businesscore.domain.port.out.CreationProduit;
import com.yowyob.businesscore.domain.port.out.GererCatalogueOffre;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/**
 * Résout le produit kernel d'une offre pour une entreprise : renvoie le {@code productId} mémorisé, ou
 * le crée (où l'organisation est connue) puis mémorise le mapping. Les identifiants kernel inconnus du
 * métier (organizationId, devise) viennent du {@link ResolveurContexteKernel} ; {@code sku}/{@code familyCode}/
 * {@code variantLabel} sont dérivés de façon déterministe depuis la {@link DefinitionOffre}.
 */
@Service
public class ProduitEntrepriseService implements ResoudreProduitEntreprise {

    private static final String FAMILLE_DEFAUT = "GENERAL";
    private static final String VARIANTE_DEFAUT = "STANDARD";

    private final DepotProduitEntreprise depotProduit;
    private final DepotOffre depotOffre;
    private final ResolveurContexteKernel resolveur;
    private final GererCatalogueOffre catalogue;

    public ProduitEntrepriseService(DepotProduitEntreprise depotProduit,
                                    DepotOffre depotOffre,
                                    ResolveurContexteKernel resolveur,
                                    GererCatalogueOffre catalogue) {
        this.depotProduit = depotProduit;
        this.depotOffre = depotOffre;
        this.resolveur = resolveur;
        this.catalogue = catalogue;
    }

    @Override
    public Mono<UUID> resoudre(UUID businessId, UUID offreId) {
        return depotProduit.trouverProductId(businessId, offreId)
                .switchIfEmpty(Mono.defer(() -> creerEtMapper(businessId, offreId)));
    }

    /** Crée le produit kernel pour cette entreprise puis mémorise le mapping (offre, entreprise) → productId. */
    private Mono<UUID> creerEtMapper(UUID businessId, UUID offreId) {
        return Mono.zip(
                        depotOffre.parId(offreId).switchIfEmpty(Mono.error(
                                ProblemException.notFound("Offre introuvable : " + offreId))),
                        resolveur.resoudre(businessId))
                .flatMap(t -> {
                    CreationProduit demande = construire(t.getT1(), t.getT2());
                    return catalogue.creerProduit(demande)
                            .flatMap(productId -> depotProduit
                                    .enregistrer(businessId, offreId, productId)
                                    .thenReturn(productId));
                });
    }

    private static CreationProduit construire(DefinitionOffre offre, ContexteKernel ctx) {
        boolean priceless = offre.prix() == null;
        BigDecimal unitPrice = priceless ? new BigDecimal("0.01") : offre.prix();
        return new CreationProduit(
                ctx.organizationId(),
                sku(offre),
                offre.nom(),
                FAMILLE_DEFAUT,
                VARIANTE_DEFAUT,
                unitPrice,
                ctx.currency(),
                offre.possede(TypeCapacite.STOCKABLE),
                priceless);
    }

    /** SKU déterministe {@code OFFRE-{slug-du-nom}-{id8}} : jamais saisi, dérivé de l'offre. */
    private static String sku(DefinitionOffre offre) {
        String slug = offre.nom().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (slug.length() > 20) {
            slug = slug.substring(0, 20);
        }
        if (slug.isBlank()) {
            slug = "OFFRE";
        }
        String id8 = offre.id().toString().substring(0, 8);
        return "OFFRE-" + slug + "-" + id8;
    }
}
