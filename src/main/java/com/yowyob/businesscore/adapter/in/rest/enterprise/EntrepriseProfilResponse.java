package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.domain.enterprise.EntrepriseProfil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Fiche produit de l'application (identité et branding)")
public record EntrepriseProfilResponse(
        UUID entrepriseId,
        String description,
        String logoUrl,
        @Schema(example = "#4F46E5") String couleur,
        String supportEmail,
        String siteWebUrl,
        @Schema(example = "PRODUCTION", allowableValues = {"DEVELOPPEMENT", "TEST", "PRODUCTION"})
        String environnement,
        Instant mettreAJourLe
) {

    public static EntrepriseProfilResponse depuis(EntrepriseProfil profil) {
        return new EntrepriseProfilResponse(
                profil.entrepriseId(),
                profil.description(),
                profil.logoUrl(),
                profil.couleur(),
                profil.supportEmail(),
                profil.siteWebUrl(),
                profil.environnement().name(),
                profil.mettreAJourLe());
    }
}
