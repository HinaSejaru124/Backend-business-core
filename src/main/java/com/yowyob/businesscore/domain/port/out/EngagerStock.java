package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — engage un mouvement de stock (sortie vente) sur le kernel.
 * Mappe : POST /api/inventory/movements puis POST .../validate.
 */
public interface EngagerStock {

  Mono<UUID> sortieVente(UUID productId, int quantite, UUID businessId, UUID commandeId);

  Mono<Void> annulerSortie(UUID productId, int quantite, UUID businessId, UUID commandeId);
}
