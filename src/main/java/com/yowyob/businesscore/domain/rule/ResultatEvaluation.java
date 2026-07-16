// domain/rule/ResultatEvaluation.java
package com.yowyob.businesscore.domain.rule;

import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import java.util.UUID;

/**
 * Résultat enrichi d'une évaluation de règle.
 *
 * <p><b>Conservé volontairement pour le Niveau 2 (N2) — non utilisé au Niveau 1.</b> Le flux
 * d'évaluation actuel retourne des {@code com.yowyob.businesscore.domain.port.internal.EffetAAppliquer}.
 * Ce record préfigure un résultat d'évaluation plus riche (N2).
 */
public record ResultatEvaluation(
        UUID idRegle,
        Effet effet,
        Declencheur declencheur,
        Object ancienneValeur,
        Object nouvelleValeur,
        String codeRegle,
        String actionRequise
) {
    public static ResultatEvaluation de(RegleMetier regle) {
        return new ResultatEvaluation(
                regle.getId(),
                regle.getEffet(),
                regle.getDeclencheur(),
                null,
                null,
                "RULE_" + regle.getId(),
                descriptionEffet(regle.getEffet())
        );
    }

    private static String descriptionEffet(Effet effet) {
        return switch (effet) {
            case BLOQUER -> "Opération refusée par une règle métier";
            case EXIGER  -> "Un document est requis avant de continuer";
            case VALIDER -> "Une approbation est requise avant de continuer";
            case AJUSTER -> "Une valeur a été corrigée automatiquement";
            case ALERTER -> "Une alerte a été émise";
            case DEROGER -> "Dérogation appliquée avec motif archivé";
        };
    }
}