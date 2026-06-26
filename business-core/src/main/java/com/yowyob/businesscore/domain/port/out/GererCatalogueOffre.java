package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port de sortie — gère les produits et prix kernel correspondant aux offres.
 * Mappe : POST /api/products ; GET /api/products/{id}/prices/effective.
 * L'adapter traduit la richesse d'une Offre (capacités, formes de prix) vers le Product kernel.
 *
 * <p>Le produit kernel est scopé organisation : la création reçoit une {@link CreationProduit} complète
 * (organizationId + champs obligatoires dérivés). Le mapping offre↔produit est mémorisé par entreprise
 * (voir {@code DepotProduitEntreprise}).
 */
public interface GererCatalogueOffre {

    Mono<UUID> creerProduit(CreationProduit demande);

    Mono<BigDecimal> prixEffectif(UUID produitId);
}
