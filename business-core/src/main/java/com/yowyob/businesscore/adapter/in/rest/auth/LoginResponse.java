package com.yowyob.businesscore.adapter.in.rest.auth;

import com.yowyob.businesscore.domain.port.out.ResultatLogin;

import java.util.List;

/**
 * Réponse de {@code POST /v1/auth/login}. Le client stocke {@code accessToken} et le rejoue en
 * {@code Authorization: Bearer} sur les appels suivants. {@code organisations} permet un éventuel choix
 * d'organisation côté client ; {@code owner} indique si l'utilisateur peut créer une organisation.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        List<String> authorities,
        List<OrganisationDto> organisations,
        boolean owner
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

    public record OrganisationDto(String organizationId, String organizationCode,
                                  String displayName, List<String> services) {
        public static OrganisationDto depuis(com.yowyob.businesscore.domain.port.out.OrganisationAccessible o) {
            return new OrganisationDto(o.organizationId(), o.organizationCode(), o.displayName(), o.services());
        }
    }
}
