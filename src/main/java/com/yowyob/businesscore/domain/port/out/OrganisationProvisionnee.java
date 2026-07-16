package com.yowyob.businesscore.domain.port.out;

import java.util.UUID;

/**
 * Références kernel produites au provisionnement d'une organisation : le business actor propriétaire
 * (créé à l'onboarding) et l'organisation. Mémorisées dans l'entité {@code Entreprise}.
 */
public record OrganisationProvisionnee(UUID businessActorId, UUID organizationId) {
}
