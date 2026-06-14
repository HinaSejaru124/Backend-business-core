package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — crée l'organisation réelle d'une entreprise.
 * Mappe : POST /api/organizations ; POST /api/organizations/{orgId}/agencies.
 */
public interface PersisterEntreprise {

    Mono<UUID> creerOrganisation(String nom);

    Mono<UUID> creerAgence(UUID organizationId, String nom);
}
