package com.yowyob.businesscore.domain.port.internal;

import java.util.Map;

/**
 * Contexte de valeurs passé à l'évaluateur de règles par la couche application.
 * Une règle ne lit jamais le kernel elle-même : toutes les valeurs nécessaires (stock, montant,
 * heure, rôle de l'acteur...) lui sont fournies ici. Garde la brique Règles pure.
 */
public record ContexteEvaluation(Map<String, Object> valeurs) {

    public ContexteEvaluation {
        valeurs = valeurs == null ? Map.of() : Map.copyOf(valeurs);
    }

    public Object get(String cle) {
        return valeurs.get(cle);
    }
}
