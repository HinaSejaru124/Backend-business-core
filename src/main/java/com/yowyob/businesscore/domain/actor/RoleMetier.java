package com.yowyob.businesscore.domain.actor;

import com.yowyob.businesscore.domain.shared.CategorieActeur;
import com.yowyob.businesscore.application.error.ProblemException;

import java.util.Objects;
import java.util.UUID;

/**
 * Rôle métier (« pharmacien responsable »), distinct du rôle technique kernel.
 * Porte la catégorie étanche (OPERATEUR / BENEFICIAIRE).
 */
public record RoleMetier(
        UUID id,
        UUID versionTypeId,
        String code,
        CategorieActeur categorie
) {
    public RoleMetier {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(versionTypeId, "versionTypeId requis");
        Objects.requireNonNull(categorie, "catégorie requise");
        if (code == null || code.isBlank()) {
            throw ProblemException.unprocessable("code du rôle métier requis");
        }
    }

    public static RoleMetier nouveau(UUID id, UUID versionTypeId, String code, CategorieActeur categorie) {
        return new RoleMetier(id, versionTypeId, code, categorie);
    }

    /** Met à jour le code ; la catégorie est immuable (RG-04). */
    public RoleMetier avecCode(String nouveauCode) {
        return new RoleMetier(id, versionTypeId, nouveauCode, categorie);
    }
}
