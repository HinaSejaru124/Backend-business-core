package com.yowyob.businesscore.domain.operation;

import com.yowyob.businesscore.domain.shared.TypeEtape;

import java.util.UUID;

/**
 * Brique 5 — une étape ordonnée d'une {@link DefinitionOperation}.
 *
 * <p>Le {@link TypeEtape} provient d'un <b>catalogue fermé</b> (discipline anti-dérive) : le développeur
 * ordonne des étapes-types, il ne programme pas. Chaque type est exécuté par une stratégie
 * {@code ExecuteurDEtape} sélectionnée par le dispatcher du socle.
 *
 * <p>Record immuable, sans dépendance technique (domaine pur).
 */
public record EtapeOperation(
        UUID id,
        UUID operationId,   // FK vers la DefinitionOperation parente
        int ordre,          // position dans la séquence (croissant)
        TypeEtape typeEtape // étape-type du catalogue
) {

    public EtapeOperation {
        if (operationId == null)
            throw new IllegalArgumentException("operationId est obligatoire");
        if (typeEtape == null)
            throw new IllegalArgumentException("typeEtape est obligatoire");
        if (ordre < 0)
            throw new IllegalArgumentException("ordre doit être >= 0");
    }

    /** Fabrique : crée une nouvelle étape avec un identifiant généré. */
    public static EtapeOperation creer(UUID operationId, int ordre, TypeEtape typeEtape) {
        return new EtapeOperation(UUID.randomUUID(), operationId, ordre, typeEtape);
    }
}
