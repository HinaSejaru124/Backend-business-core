package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port de sortie — gère les produits et prix kernel correspondant aux offres.
 * Mappe : POST /api/products ; GET /api/products/{id}/prices/effective.
 * L'adapter traduit la richesse d'une Offre (capacités, formes de prix) vers le Product kernel.
 */
public interface GererCatalogueOffre {

    Mono<UUID> creerProduit(String nom, boolean stockable, BigDecimal prix);

    Mono<BigDecimal> prixEffectif(UUID produitId);
}
