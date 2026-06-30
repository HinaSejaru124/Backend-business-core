package com.yowyob.businesscore.domain.port.out;

import java.util.List;

/**
 * Résultat d'une authentification au kernel ({@code POST /api/auth/login}). Le BC renvoie l'accessToken
 * au client (qui le rejouera en {@code Bearer}) ; il ne stocke pas de mot de passe. Pas de refresh token
 * pour l'instant (re-login à expiration).
 */
public record ResultatLogin(
        String accessToken,
        long expiresInSeconds,
        List<String> authorities,
        List<OrganisationAccessible> organisations,
        String tenantId,
        String actorId
) {
    public ResultatLogin {
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
        organisations = organisations == null ? List.of() : List.copyOf(organisations);
    }

    /** OWNER = présence de la permission de création d'organisation dans le token. */
    public boolean estOwner() {
        return authorities.contains("organizations:write");
    }
}
