// application/usecase/rule/EvaluateurConditionN1.java
package com.yowyob.businesscore.application.usecase.rule;

import com.yowyob.businesscore.domain.port.internal.ContexteEvaluation;
import org.springframework.stereotype.Component;

/**
 * Évalue si une condition paramétrée (N1 — catalogue fermé) est satisfaite.
 * Format de condition : "TYPE_CONDITION:parametre=valeur[,parametre=valeur...]"
 * Exemples :
 *   "TOUJOURS_VRAI"
 *   "CATEGORIE_EGALE:valeur=medicament_prescription"
 *   "MONTANT_SUPERIEUR:seuil=1000"
 */
@Component
public class EvaluateurConditionN1 {

    public boolean satisfait(String condition, ContexteEvaluation ctx) {
        if (condition == null || condition.isBlank()) return false;

        String[] parts = condition.split(":", 2);
        String type = parts[0].trim();
        String params = parts.length > 1 ? parts[1] : "";

        return switch (type) {
            case "TOUJOURS_VRAI" -> true;

            case "CATEGORIE_EGALE" -> {
                String attendu = extraireParam(params, "valeur");
                Object valeur = ctx.get("categorie");
                yield attendu != null && attendu.equals(String.valueOf(valeur));
            }

            case "MONTANT_SUPERIEUR" -> {
                String seuilStr = extraireParam(params, "seuil");
                Object valeur = ctx.get("montant");
                yield seuilStr != null && valeur != null
                        && toDouble(valeur) > Double.parseDouble(seuilStr);
            }

            case "MONTANT_INFERIEUR" -> {
                String seuilStr = extraireParam(params, "seuil");
                Object valeur = ctx.get("montant");
                yield seuilStr != null && valeur != null
                        && toDouble(valeur) < Double.parseDouble(seuilStr);
            }

            // Type inconnu au N1 : non applicable (N2 gérera)
            default -> false;
        };
    }

    private String extraireParam(String params, String cle) {
        for (String token : params.split(",")) {
            String[] kv = token.split("=", 2);
            if (kv.length == 2 && kv[0].trim().equals(cle)) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }
}