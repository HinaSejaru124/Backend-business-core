package com.yowyob.businesscore.domain.port.out.offer;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port kernel catalogue. TRADUIT notre Offre (plus riche) vers le Product kernel.
 * Impl : POST /api/products, GET /api/products/{id}/prices/effective.
 */
public interface GererCatalogueOffre {
    /** Traduit + publie l'offre comme Product ; renvoie le productId kernel. */
    Mono<UUID> publierCommeProduit(DefinitionOffre offre);

    /** Prix effectif courant côté kernel. */
    Mono<BigDecimal> prixEffectif(UUID productId);
}
