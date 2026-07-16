// domain/rule/ConditionRegle.java
package com.yowyob.businesscore.domain.rule;

import java.util.Map;

/**
 * Condition structurée (type + paramètres).
 *
 * <p><b>Conservé volontairement pour le Niveau 2 (N2) — non utilisé au Niveau 1.</b> Le design retenu
 * au N1 encode la condition sous forme de chaîne ({@code RegleMetier.condition}), parsée par
 * {@code EvaluateurConditionN1}. Ce record préfigure une condition structurée et composable (N2).
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