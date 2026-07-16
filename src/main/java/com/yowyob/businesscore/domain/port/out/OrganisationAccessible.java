package com.yowyob.businesscore.domain.port.out;

import java.util.List;

/**
 * Organisation à laquelle l'utilisateur authentifié a accès, telle que projetée par le kernel au login
 * ({@code LoginResponse.organizations[]}). Sert à proposer un choix d'organisation côté client.
 */
public record OrganisationAccessible(
        String organizationId,
        String organizationCode,
        String displayName,
        List<String> services
) {
    public OrganisationAccessible {
        services = services == null ? List.of() : List.copyOf(services);
    }
}
