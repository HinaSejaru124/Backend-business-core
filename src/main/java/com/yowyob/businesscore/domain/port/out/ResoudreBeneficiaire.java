package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — résout/crée un bénéficiaire externe (tiers).
 *
 * <p>Un tiers kernel n'est pas autonome : il enveloppe une « partie » (actor/organisation). Le
 * bénéficiaire est donc matérialisé en deux temps — création de l'actor support puis déclaration du
 * tiers ({@code POST /api/actors} puis {@code POST /api/third-parties}, sous l'organisation). RG-04 :
 * le bénéficiaire reste un tiers (rôle commercial), distinct d'un opérateur (accès système).
 *
 * @param organizationId organisation sous laquelle déclarer le tiers (du résolveur de contexte).
 */
public interface ResoudreBeneficiaire {

    Mono<UUID> resoudreBeneficiaire(UUID organizationId, String identifiant, String nom);
}
