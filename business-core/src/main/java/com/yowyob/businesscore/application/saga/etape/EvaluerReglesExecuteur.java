package com.yowyob.businesscore.application.saga.etape;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.saga.ClesContexte;
import com.yowyob.businesscore.application.usecase.rule.GestionnaireEffets;
import com.yowyob.businesscore.domain.port.internal.ContexteEtape;
import com.yowyob.businesscore.domain.port.internal.ContexteEvaluation;
import com.yowyob.businesscore.domain.port.internal.EffetAAppliquer;
import com.yowyob.businesscore.domain.port.internal.EvaluateurDeRegle;
import com.yowyob.businesscore.domain.port.internal.ExecuteurDEtape;
import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.TypeEtape;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Étape {@code EVALUER_REGLES} — point d'ancrage des règles dans le workflow.
 *
 * <p>Appelle le port interne {@link EvaluateurDeRegle} (implémenté par Dev 4 — jamais sa classe en
 * direct) en lui passant un {@link ContexteEvaluation} de valeurs déjà résolues par les étapes
 * précédentes (stock, montant, catégorie, rôles...). La couche Opérations ne connaît rien de la
 * mécanique d'évaluation.
 *
 * <p>Réaction aux effets : un effet <b>bloquant</b> (BLOQUER / EXIGER / VALIDER) interrompt l'opération
 * avec un {@code 422} RFC 7807 enrichi ({@code violatedRule}, {@code requiredAction},
 * {@code requiredDocument}). Les effets traçants/mutateurs sont conservés dans le contexte pour audit.
 */
@Component
public class EvaluerReglesExecuteur implements ExecuteurDEtape {

    private final EvaluateurDeRegle evaluateurDeRegle;
    private final GestionnaireEffets gestionnaireEffets;

    public EvaluerReglesExecuteur(EvaluateurDeRegle evaluateurDeRegle, GestionnaireEffets gestionnaireEffets) {
        this.evaluateurDeRegle = evaluateurDeRegle;
        this.gestionnaireEffets = gestionnaireEffets;
    }

    @Override
    public TypeEtape typeSupporte() {
        return TypeEtape.EVALUER_REGLES;
    }

    @Override
    public Mono<ContexteEtape> executer(ContexteEtape contexte) {
        Declencheur declencheur = declencheurDe(contexte);
        ContexteEvaluation contexteEval = new ContexteEvaluation(valeursPour(contexte));

        return evaluateurDeRegle.evaluer(declencheur, contexteEval)
                .collectList()
                .flatMap(effets -> {
                    EffetAAppliquer bloquant = premierBloquant(effets);
                    if (bloquant != null) {
                        return Mono.error(versProbleme(bloquant));
                    }
                    // Effets non bloquants appliqués (audit AJUSTER, événement ALERTER, cascade DEROGER),
                    // puis on conserve la liste dans le contexte pour la trace.
                    return gestionnaireEffets.appliquerNonBloquants(effets, rolesDe(contexte))
                            .thenReturn(contexte.avec(ClesContexte.RESULTAT_REGLES, effets));
                });
    }

    @SuppressWarnings("unchecked")
    private Set<String> rolesDe(ContexteEtape contexte) {
        Object roles = contexte.get(ClesContexte.ROLES);
        if (roles instanceof Set<?> ensemble) {
            return (Set<String>) ensemble;
        }
        return Set.of();
    }

    private Declencheur declencheurDe(ContexteEtape contexte) {
        Object valeur = contexte.get(ClesContexte.DECLENCHEUR);
        if (valeur instanceof Declencheur d) return d;
        if (valeur != null) return Declencheur.valueOf(valeur.toString());
        return Declencheur.AVANT_OPERATION;
    }

    /** Valeurs transmises aux conditions de règles (la règle ne lit jamais le kernel elle-même). */
    private Map<String, Object> valeursPour(ContexteEtape contexte) {
        Map<String, Object> valeurs = new HashMap<>();
        copier(contexte, valeurs, ClesContexte.ENTREPRISE_ID);
        copier(contexte, valeurs, ClesContexte.VERSION_TYPE_ID);
        copier(contexte, valeurs, ClesContexte.MONTANT);
        copier(contexte, valeurs, ClesContexte.CATEGORIE);
        copier(contexte, valeurs, ClesContexte.STOCK);
        copier(contexte, valeurs, ClesContexte.QUANTITE);
        copier(contexte, valeurs, ClesContexte.ROLES);
        copier(contexte, valeurs, ClesContexte.OFFRE_ID);
        copier(contexte, valeurs, ClesContexte.MOTIF);
        return valeurs;
    }

    private void copier(ContexteEtape source, Map<String, Object> cible, String cle) {
        Object valeur = source.get(cle);
        if (valeur != null) {
            cible.put(cle, valeur);
        }
    }

    private EffetAAppliquer premierBloquant(List<EffetAAppliquer> effets) {
        return effets.stream().filter(e -> e.effet().estBloquant()).findFirst().orElse(null);
    }

    private ProblemException versProbleme(EffetAAppliquer effet) {
        ProblemException probleme = ProblemException.unprocessable(
                        effet.message() != null ? effet.message() : "Opération refusée par une règle métier")
                .requiredAction(effet.effet().name());
        if (effet.regleId() != null) {
            probleme.violatedRule(effet.regleId().toString());
        }
        Object document = effet.details().get("document");
        if (document != null) {
            probleme.requiredDocument(document.toString());
        }
        return probleme;
    }
}
