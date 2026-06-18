package com.yowyob.businesscore.domain.offer;

import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import com.yowyob.businesscore.shared.error.ProblemException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Unité de valeur proposée. Modèle hybride : socle commun (nom, formePrix, prix)
 * + capacités activables combinables.
 * Invariant prix : FIXE exige un montant > 0 ; GRATUIT / SUR_DEVIS interdisent un montant.
 */
public record DefinitionOffre(
        UUID id,
        UUID versionTypeId,
        String nom,
        FormePrix formePrix,
        BigDecimal prix,
        List<Capacite> capacites
) {
    public DefinitionOffre {
        Objects.requireNonNull(id, "id requis");
        Objects.requireNonNull(versionTypeId, "versionTypeId requis");
        Objects.requireNonNull(formePrix, "formePrix requise");
        if (nom == null || nom.isBlank()) {
            throw ProblemException.unprocessable("nom de l'offre requis");
        }
        if (formePrix.exigeMontant()) {
            if (prix == null || prix.signum() <= 0) {
                throw ProblemException.unprocessable("un prix FIXE exige un montant strictement positif");
            }
        } else if (prix != null) {
            throw ProblemException.unprocessable("une offre " + formePrix + " ne peut pas porter de montant");
        }
        capacites = capacites == null ? List.of() : List.copyOf(capacites);
    }

    public static DefinitionOffre nouvelle(UUID id, UUID versionTypeId, String nom,
                                           FormePrix formePrix, BigDecimal prix) {
        return new DefinitionOffre(id, versionTypeId, nom, formePrix, prix, List.of());
    }

    public DefinitionOffre avecCapacites(List<Capacite> nouvelles) {
        return new DefinitionOffre(id, versionTypeId, nom, formePrix, prix, nouvelles);
    }

    public boolean possede(TypeCapacite type) {
        return capacites.stream().anyMatch(c -> c.type() == type && c.active());
    }
}
