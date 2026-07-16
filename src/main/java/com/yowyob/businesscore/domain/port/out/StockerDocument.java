package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — stocke une pièce (ordonnance, licence...).
 * Mappe : POST /api/files ; GET /api/files/{id}/content.
 */
public interface StockerDocument {

    Mono<UUID> stocker(String nom, String contentType, byte[] contenu);

    Mono<byte[]> lireContenu(UUID fichierId);
}
