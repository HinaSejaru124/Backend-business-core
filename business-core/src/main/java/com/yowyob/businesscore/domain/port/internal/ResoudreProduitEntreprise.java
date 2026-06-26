package com.yowyob.businesscore.domain.port.internal;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interne — résout l'identifiant du <b>produit kernel</b> correspondant à une offre, pour une
 * entreprise donnée. Crée le produit paresseusement (là où l'organisation est connue) au premier appel
 * et mémorise le mapping ; les appels suivants renvoient le {@code productId} mémorisé.
 *
 * <p>Découple le métier (qui ne manipule que l'offre) du kernel (Product scopé organisation).
 */
public interface ResoudreProduitEntreprise {

    Mono<UUID> resoudre(UUID businessId, UUID offreId);
}
