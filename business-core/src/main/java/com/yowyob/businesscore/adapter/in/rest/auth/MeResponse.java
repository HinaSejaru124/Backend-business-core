package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.application.context.BusinessContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Profil de l'utilisateur authentifié (claims JWT kernel + identité développeur BC)")
public record MeResponse(
        @Schema(description = "Tenant kernel (`tid`)") UUID tenantId,
        @Schema(description = "Acteur kernel (`actor`)") UUID actorId,
        @Schema(description = "Permissions dérivées du JWT") List<String> permissions,
        @Schema(description = "Présence de `organizations:write`") boolean owner,
        @Schema(description = "Identifiant développeur BC stable — public, ne change jamais, "
                + "distinct des clés API (secrètes, scopées à une entreprise)")
        UUID developerId,
        String email,
        @Schema(example = "FREE") String plan
) {

    public static MeResponse depuis(BusinessContext ctx, UUID developerId, String email, String plan) {
        return new MeResponse(
                ctx.tenantId(),
                ctx.actorId(),
                List.copyOf(ctx.roles()),
                ctx.hasRole("organizations:write"),
                developerId,
                email,
                plan);
    }
}
