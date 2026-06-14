package com.yowyob.businesscore.domain.port.internal;

import com.yowyob.businesscore.domain.shared.TypeEtape;
import reactor.core.publisher.Mono;

/**
 * Port interne (stratégie) — exécute une étape-type d'opération.
 *
 * <p>Chaque étape-type du catalogue est une implémentation distincte. Le dispatcher du socle
 * sélectionne l'implémentation via {@link #typeSupporte()}. Ajouter une étape = ajouter une
 * implémentation, sans toucher au moteur.
 */
public interface ExecuteurDEtape {

    TypeEtape typeSupporte();

    Mono<ContexteEtape> executer(ContexteEtape contexte);
}
