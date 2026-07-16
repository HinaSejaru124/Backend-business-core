package com.yowyob.businesscore.domain.port.internal;

import com.yowyob.businesscore.domain.shared.Declencheur;
import reactor.core.publisher.Flux;

/**
 * Port interne (stratégie) — évalue les règles applicables à un déclencheur et retourne les effets.
 *
 * <p>Mécanisme d'extensibilité : l'implémentation de départ est le Niveau 1 (catalogue paramétré) ;
 * une implémentation Niveau 2 (conditions composables) pourra coexister sans réécriture. La couche
 * application appelle ce port aux points d'ancrage et lui fournit le {@link ContexteEvaluation}.
 */
public interface EvaluateurDeRegle {

    Flux<EffetAAppliquer> evaluer(Declencheur declencheur, ContexteEvaluation contexte);
}
