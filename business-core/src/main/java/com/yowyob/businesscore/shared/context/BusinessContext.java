package com.yowyob.businesscore.shared.context;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ⚠️ STUB DE SOCLE — présent uniquement pour le mode standalone.
 * Si le vrai socle est disponible, SUPPRIME ce fichier et importe le BusinessContext du socle.
 *
 * Contexte métier de la requête courante, tel que décrit dans CONVENTIONS-SOCLE.md.
 */
public record BusinessContext(
        UUID tenantId,
        UUID actorId,
        List<String> roles,
        UUID businessId,
        String traceId,
        Locale locale
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
