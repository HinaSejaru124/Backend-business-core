// application/usecase/rule/EvaluateurDeRegleN1.java
package com.yowyob.businesscore.application.usecase.rule;

import com.yowyob.businesscore.domain.port.internal.ContexteEvaluation;
import com.yowyob.businesscore.domain.port.internal.EffetAAppliquer;
import com.yowyob.businesscore.domain.port.internal.EvaluateurDeRegle;
import com.yowyob.businesscore.domain.port.out.RegistreDeRegles;
import com.yowyob.businesscore.domain.port.out.RegleChargee;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Niveau 1 : catalogue fermé de conditions/effets paramétrées.
 * Qualifié "evaluateurN1" pour coexister avec une future impl N2 sans réécriture.
 */
@Component("evaluateurN1")
public class EvaluateurDeRegleN1 implements EvaluateurDeRegle {

    private final RegistreDeRegles registre;
    private final EvaluateurConditionN1 evaluateurCondition;

    public EvaluateurDeRegleN1(RegistreDeRegles registre,
                                EvaluateurConditionN1 evaluateurCondition) {
        this.registre = registre;
        this.evaluateurCondition = evaluateurCondition;
    }

    /**
     * Pipeline réactif en trois temps : <b>charger</b> les règles du déclencheur (Type + locales),
     * <b>filtrer</b> celles dont la condition N1 est satisfaite par le contexte, puis <b>convertir</b>
     * chaque règle retenue en effet à appliquer. Une règle non satisfaite ne produit aucun effet.
     */
    @Override
    public Flux<EffetAAppliquer> evaluer(Declencheur declencheur, ContexteEvaluation contexte) {
        // entrepriseId facultatif : transmis dans les valeurs du contexte, accepté en UUID ou en String.
        Object entrepriseIdObj = contexte.get("entrepriseId");
        java.util.UUID entrepriseId = entrepriseIdObj instanceof java.util.UUID u ? u
                : entrepriseIdObj != null ? java.util.UUID.fromString(entrepriseIdObj.toString())
                : null;

        return registre.chargerPourDeclencheur(entrepriseId, declencheur)
                .filter(regle -> evaluateurCondition.satisfait(regle.condition(), contexte))
                .map(regle -> toEffet(regle, contexte));
    }

    private EffetAAppliquer toEffet(RegleChargee regle, ContexteEvaluation contexte) {
        if (regle.effet() == Effet.AJUSTER) {
            // AJUSTER : on trace ancienne valeur dans les détails
            Object ancienne = contexte.get("valeurCible");
            return new EffetAAppliquer(
                    regle.effet(),
                    regle.id(),
                    "Valeur ajustée automatiquement",
                    Map.of("ancienneValeur", ancienne != null ? ancienne : "N/A")
            );
        }
        return new EffetAAppliquer(regle.effet(), regle.id(),
                descriptionEffet(regle.effet()), Map.of());
    }

    private String descriptionEffet(Effet effet) {
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