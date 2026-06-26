package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — lit le stock disponible d'un produit kernel.
 * Mappe : GET /api/inventory/movements/balance (3 query params obligatoires côté kernel :
 * {@code organizationId}, {@code agencyId}, {@code productId}).
 *
 * <p>L'appelant fournit le {@code productId} kernel (résolu via {@code ResoudreProduitEntreprise}) et le
 * {@code businessId} ; l'adapter résout {@code organizationId}/{@code agencyId} via le
 * {@code ResolveurContexteKernel} — le métier ne les connaît pas.
 */
public interface VerifierDisponibilite {

    Mono<Long> soldeStock(UUID productId, UUID businessId);
}
