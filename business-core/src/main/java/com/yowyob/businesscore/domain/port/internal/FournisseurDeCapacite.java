package com.yowyob.businesscore.domain.port.internal;

import com.yowyob.businesscore.domain.shared.TypeCapacite;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port interne (stratégie) — active une capacité d'offre (STOCKABLE, PLANIFIABLE...).
 * Le dispatcher du socle sélectionne l'implémentation via {@link #typeSupporte()}.
 */
public interface FournisseurDeCapacite {

    TypeCapacite typeSupporte();

    Mono<Void> activer(UUID offreId);
}
