package com.yowyob.businesscore.adapter.in.rest.enterprise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informations générales (fiche produit) d'une application")
public record ModifierProfilRequest(
        @Schema(description = "Description courte de l'application", example = "Point de vente pharmacie")
        String description,
        @Schema(description = "URL du logo", example = "https://cdn.example.com/logo.png") String logoUrl,
        @Schema(description = "Couleur d'accent (hex #RRGGBB)", example = "#4F46E5") String couleur,
        @Schema(description = "E-mail de support", example = "support@mon-app.example.com") String supportEmail,
        @Schema(description = "Site web de l'application", example = "https://mon-app.example.com") String siteWebUrl,
        @Schema(description = "Environnement de déploiement", example = "PRODUCTION",
                allowableValues = {"DEVELOPPEMENT", "TEST", "PRODUCTION"})
        String environnement
) {
}
