package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — résout/crée un bénéficiaire externe (tiers).
 * Mappe : POST /api/third-parties.
 * Étanchéité RG-04 : un bénéficiaire vit dans tp-core, jamais dans actor-core.
 */
public interface ResoudreBeneficiaire {

    Mono<UUID> resoudreBeneficiaire(String identifiant, String nom);
}
