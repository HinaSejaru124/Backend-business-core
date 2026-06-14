package com.yowyob.businesscore.application.context;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Contexte métier propagé à tous les use cases (figé par le socle).
 *
 * <p>Porte l'identité d'acheminement d'une requête authentifiée :
 * <ul>
 *   <li>{@code tenantId} : le développeur (= ClientApplication kernel). Frontière d'isolation dure (RLS).</li>
 *   <li>{@code actorId} : l'opérateur asserté par le backend du développeur (on-behalf-of), optionnel.</li>
 *   <li>{@code roles} : rôles métier de l'acteur courant (autorisation, DEROGER).</li>
 *   <li>{@code businessId} : entreprise ciblée, optionnelle (sous-cloisonnement applicatif).</li>
 *   <li>{@code traceId} : corrélation/observabilité.</li>
 *   <li>{@code locale} : langue de la requête.</li>
 * </ul>
 *
 * Le {@code tenantId} ne provient JAMAIS du payload client : il est dérivé de la clé Business Core
 * lors de l'authentification.
 */
public record BusinessContext(
        UUID tenantId,
        UUID actorId,
        Set<String> roles,
        UUID businessId,
        String traceId,
        Locale locale
) {
    public BusinessContext {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId est obligatoire dans le BusinessContext");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        locale = locale == null ? Locale.getDefault() : locale;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
