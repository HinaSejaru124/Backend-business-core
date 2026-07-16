package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Réponse de login — JWT à utiliser en Bearer")
public record LoginResponse(
        @Schema(description = "JWT kernel") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Durée de validité en secondes", example = "3600") long expiresInSeconds,
        List<String> authorities,
        List<OrganisationDto> organisations,
        @Schema(description = "Peut créer une organisation") boolean owner
) {

    public static LoginResponse depuis(ResultatLogin r) {
        return new LoginResponse(
                r.accessToken(),
                "Bearer",
                r.expiresInSeconds(),
                r.authorities(),
                r.organisations().stream().map(OrganisationDto::depuis).toList(),
                r.estOwner());
    }

    @Schema(description = "Organisation accessible après login")
    public record OrganisationDto(
            @Schema(example = "org-123") String organizationId,
            @Schema(example = "ALPHA") String organizationCode,
            @Schema(example = "Boutique Alpha") String displayName,
            List<String> services
    ) {
        public static OrganisationDto depuis(com.yowyob.businesscore.domain.port.out.OrganisationAccessible o) {
            return new OrganisationDto(o.organizationId(), o.organizationCode(), o.displayName(), o.services());
        }
    }
}
