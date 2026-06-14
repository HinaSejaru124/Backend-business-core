package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — lit le stock disponible d'une offre stockable.
 * Mappe : GET /api/inventory/movements/balance.
 */
public interface VerifierDisponibilite {

    Mono<Long> soldeStock(UUID produitId);
}
