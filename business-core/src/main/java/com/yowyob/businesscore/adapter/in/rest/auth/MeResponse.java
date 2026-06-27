package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.application.context.BusinessContext;

import java.util.List;
import java.util.UUID;

/**
 * Réponse de {@code GET /v1/auth/me} : identité de l'utilisateur authentifié, dérivée des claims du JWT
 * kernel ({@code tid}, {@code actor}, {@code permissions}). {@code owner} = présence de
 * {@code organizations:write}.
 */
public record MeResponse(
        UUID tenantId,
        UUID actorId,
        List<String> permissions,
        boolean owner
) {

    public static MeResponse depuis(BusinessContext ctx) {
        return new MeResponse(
                ctx.tenantId(),
                ctx.actorId(),
                List.copyOf(ctx.roles()),
                ctx.hasRole("organizations:write"));
    }
}
