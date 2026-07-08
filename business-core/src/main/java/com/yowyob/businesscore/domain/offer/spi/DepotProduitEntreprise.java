package com.yowyob.businesscore.domain.offer.spi;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de persistance locale du mapping <b>offre ↔ produit kernel</b>, matérialisé par entreprise.
 *
 * <p>Le produit kernel est scopé organisation alors qu'une offre est déclarée au niveau Type/version :
 * on mémorise donc, pour chaque {@code (entrepriseId, offreId)}, l'identifiant du produit kernel créé.
 * Cela évite de recréer le produit à chaque opération et fournit le {@code productId} attendu par le
 * core inventory.
 */
public interface DepotProduitEntreprise {

    /** Identifiant du produit kernel déjà mappé pour cette entreprise et cette offre, s'il existe. */
    Mono<UUID> trouverProductId(UUID entrepriseId, UUID offreId);

    /** Mémorise le produit kernel créé pour {@code (entrepriseId, offreId)} (tenant courant). */
    Mono<Void> enregistrer(UUID entrepriseId, UUID offreId, UUID productId);

    /** Indique si au moins une entreprise a déjà un produit kernel mappé sur cette offre. */
    Mono<Boolean> existeMappingPourOffre(UUID offreId);
}
