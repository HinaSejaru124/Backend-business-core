// domain/rule/ResultatEvaluation.java
package com.yowyob.businesscore.domain.rule;

import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import java.util.UUID;

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