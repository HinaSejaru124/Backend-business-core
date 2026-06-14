package com.yowyob.businesscore.domain.businesstype;

import com.yowyob.businesscore.domain.shared.StatutType;

import java.util.UUID;

/**
 * Brique 1 — Type Métier (coquille socle).
 *
 * <p>Gabarit réutilisable d'une catégorie d'activité, propriété d'un développeur ({@code tenantId}).
 * Le socle fournit cette forme stable ; Dev 2 enrichit la logique (publication de versions, épinglage,
 * statut). Domaine pur : aucune dépendance technique.
 */
public record TypeMetier(
        UUID id,
        UUID tenantId,
        UUID businessDomainId,
        String code,
        String nom,
        StatutType statut
) {
    public static TypeMetier creer(UUID tenantId, String code, String nom, UUID businessDomainId) {
        return new TypeMetier(UUID.randomUUID(), tenantId, businessDomainId, code, nom, StatutType.BROUILLON);
    }
}
