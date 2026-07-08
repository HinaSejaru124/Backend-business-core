package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.application.context.BusinessContext;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Profil de l'utilisateur authentifié (claims JWT kernel)")
public record MeResponse(
        @Schema(description = "Tenant kernel (`tid`)") UUID tenantId,
        @Schema(description = "Acteur kernel (`actor`)") UUID actorId,
        @Schema(description = "Permissions dérivées du JWT") List<String> permissions,
        @Schema(description = "Présence de `organizations:write`") boolean owner
) {

    public static MeResponse depuis(BusinessContext ctx) {
        return new MeResponse(
                ctx.tenantId(),
                ctx.actorId(),
                List.copyOf(ctx.roles()),
                ctx.hasRole("organizations:write"));
    }
}
