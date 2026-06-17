// domain/rule/ConditionRegle.java
package com.yowyob.businesscore.domain.rule;

import java.util.Map;

/**
 * Niveau 1 : condition paramétrée (catalogue fermé).
 * Conçue pour être remplacée par une impl plus riche au N2
 * sans changer la signature de RegleMetier.
 */
public record ConditionRegle(
    String typeCondition,           // ex. "CATEGORIE_EGALE", "MONTANT_SUPERIEUR"
    Map<String, Object> parametres  // les valeurs à comparer
) {
    public ConditionRegle {
        if (typeCondition == null || typeCondition.isBlank()) {
            throw new IllegalArgumentException("typeCondition obligatoire");
        }
        parametres = parametres != null ? Map.copyOf(parametres) : Map.of();
    }
}