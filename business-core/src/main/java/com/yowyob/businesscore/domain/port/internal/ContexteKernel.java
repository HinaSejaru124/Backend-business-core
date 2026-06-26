package com.yowyob.businesscore.domain.port.internal;

import java.util.UUID;

/**
 * Identifiants kernel résolus pour une entreprise, que le métier ne modélise pas directement et dont
 * les adapters kernel ont besoin (produit, stock, caisse, vente). Assemblé par
 * {@link ResolveurContexteKernel} à partir du {@code BusinessContext}, de l'entité {@code Entreprise},
 * de la Configuration et, au besoin, d'un appel kernel (agence principale).
 *
 * @param organizationId  organisation kernel de l'entreprise (depuis l'entité).
 * @param agencyId        agence principale (mémorisée ou résolue via le kernel) — peut être null.
 * @param businessActorId business actor propriétaire (mémorisé à l'onboarding) — peut être null.
 * @param registerId      caisse à utiliser (Configuration {@code caisse_principale}) — peut être null.
 * @param cashierId       caissier = acteur courant ({@code BusinessContext.actorId}) — peut être null.
 * @param currency        devise (Configuration {@code devise}, défaut {@code XAF}).
 */
public record ContexteKernel(
        UUID organizationId,
        UUID agencyId,
        UUID businessActorId,
        UUID registerId,
        UUID cashierId,
        String currency
) {
}
