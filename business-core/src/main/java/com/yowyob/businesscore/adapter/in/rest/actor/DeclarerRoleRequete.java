package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.shared.CategorieActeur;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /v1/business-types/{typeId}/versions/{n}/roles}. La version cible provient de
 * l'URL ; le corps ne porte que le code du rôle métier et sa catégorie (OPERATEUR / BENEFICIAIRE).
 */
public record DeclarerRoleRequete(
        @NotBlank String code,
        @NotNull CategorieActeur categorie) {
}
