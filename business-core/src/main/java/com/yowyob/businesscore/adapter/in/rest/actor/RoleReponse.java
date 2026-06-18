package com.yowyob.businesscore.adapter.in.rest.actor;

import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.shared.CategorieActeur;

import java.util.UUID;

/** Réponse d'un rôle métier déclaré (aligné OpenAPI {@code Role}). */
public record RoleReponse(UUID id, UUID versionTypeId, String code, CategorieActeur categorie) {

    public static RoleReponse de(RoleMetier r) {
        return new RoleReponse(r.id(), r.versionTypeId(), r.code(), r.categorie());
    }
}
