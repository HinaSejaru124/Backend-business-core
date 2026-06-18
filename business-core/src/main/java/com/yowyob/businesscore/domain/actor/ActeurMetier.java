package com.yowyob.businesscore.domain.actor;

import com.yowyob.businesscore.shared.error.ProblemException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Association (personne + rôle métier + entreprise).
 * acteurKernelId référence soit un Actor (opérateur) soit un Tiers (bénéficiaire) du kernel.
 * RG-04 : on ne mute jamais la catégorie ; un changement de statut crée un NOUVEL ActeurMetier.
 */
public record ActeurMetier(
        UUID id,
        UUID entrepriseId,
        UUID roleMetierId,
        UUID acteurKernelId,
        Instant valideDepuis,
        Instant valideJusqua
) {
    public ActeurMetier {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(entrepriseId, "entrepriseId requis");
        Objects.requireNonNull(roleMetierId, "roleMetierId requis");
        Objects.requireNonNull(acteurKernelId, "acteurKernelId requis");
        Objects.requireNonNull(valideDepuis, "valideDepuis requis");
        if (valideJusqua != null && valideJusqua.isBefore(valideDepuis)) {
            throw ProblemException.unprocessable("valideJusqua ne peut pas précéder valideDepuis");
        }
    }

    public static ActeurMetier nouveau(UUID id, UUID entrepriseId, UUID roleMetierId, UUID acteurKernelId) {
        return new ActeurMetier(id, entrepriseId, roleMetierId, acteurKernelId, Instant.now(), null);
    }

    public boolean estActif() {
        return valideJusqua == null;
    }

    /** Détache l'acteur (fin de validité) — on ne supprime pas, on clôt. */
    public ActeurMetier detacher(Instant a) {
        return new ActeurMetier(id, entrepriseId, roleMetierId, acteurKernelId, valideDepuis, a);
    }
}
