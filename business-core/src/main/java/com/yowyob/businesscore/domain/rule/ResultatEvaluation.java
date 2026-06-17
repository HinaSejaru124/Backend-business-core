// domain/rule/ResultatEvaluation.java
package com.yowyob.businesscore.domain.rule;

import com.yowyob.businesscore.domain.shared.Effet;
import java.util.UUID;

public record ResultatEvaluation(
    UUID idRegle,
    Effet effet,
    String declencheur,
    Object ancienneValeur,   // renseigné si AJUSTER
    Object nouvelleValeur,   // renseigné si AJUSTER
    String codeRegle,        // pour le corps RFC 7807
    String actionRequise     // description lisible de ce qu'il faut faire
) {
    public static ResultatEvaluation de(RegleMetier regle, ContexteEvaluation ctx) {
        return new ResultatEvaluation(
            regle.getId(),
            regle.getEffet(),
            regle.getDeclencheur(),
            null, null,
            "RULE_" + regle.getId(),
            descriptionEffet(regle.getEffet())
        );
    }

    private static String descriptionEffet(Effet effet) {
        return switch (effet) {
            case BLOQUER  -> "Opération refusée par une règle métier";
            case EXIGER   -> "Un document est requis avant de continuer";
            case VALIDER  -> "Une approbation est requise avant de continuer";
            case AJUSTER  -> "Une valeur a été corrigée automatiquement";
            case ALERTER  -> "Une alerte a été émise";
            case DEROGER  -> "Dérogation appliquée avec motif archivé";
        };
    }
}