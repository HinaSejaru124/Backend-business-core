package com.yowyob.businesscore.domain.port.out.offer;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port kernel inventaire (offres STOCKABLE).
 * Impl : GET /api/inventory/movements/balance.
 */
public interface VerifierDisponibilite {
    Mono<BigDecimal> soldeDisponible(UUID productId);
}
