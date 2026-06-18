package com.yowyob.businesscore.domain.port.out.enterprise;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port kernel : crée l'organisation puis l'agence par défaut.
 * Impl : POST /api/organizations puis POST /api/organizations/{orgId}/agencies.
 * Renvoie l'organizationId kernel à épingler sur l'Entreprise.
 */
public interface PersisterEntreprise {
    Mono<UUID> creerOrganisationAvecAgence(String nomLocal);
}
