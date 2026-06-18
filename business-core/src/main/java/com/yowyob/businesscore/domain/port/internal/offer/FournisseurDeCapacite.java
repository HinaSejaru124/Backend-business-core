package com.yowyob.businesscore.domain.port.internal.offer;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import reactor.core.publisher.Mono;

/**
 * Stratégie d'activation d'une capacité (même principe d'extensibilité que les règles N1->N2).
 * Chaque TypeCapacite a sa propre implémentation. STOCKABLE en premier ; les autres suivent.
 */
public interface FournisseurDeCapacite {
    TypeCapacite type();

    /** Exécute l'effet d'activation de la capacité pour l'offre donnée. */
    Mono<Void> activer(DefinitionOffre offre);
}
