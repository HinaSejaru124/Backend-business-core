package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.shared.CategorieActeur;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Déclaration d'un rôle métier sur une version")
public record DeclarerRoleRequete(
        @Schema(description = "Code unique du rôle", example = "CAISSIER")
        @NotBlank String code,
        @Schema(description = "Catégorie d'acteur", example = "OPERATEUR")
        @NotNull CategorieActeur categorie
) {
}
