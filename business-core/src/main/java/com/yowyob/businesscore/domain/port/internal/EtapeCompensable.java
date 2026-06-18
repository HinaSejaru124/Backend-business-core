package com.yowyob.businesscore.domain.port.internal;

import reactor.core.publisher.Mono;

/**
 * Étape d'opération capable d'<b>annuler son propre effet</b> (compensation de saga orchestrée).
 *
 * <p>Le kernel RT-Comops n'expose pas de moteur Saga : la compensation est pilotée localement par le
 * {@code MoteurOperation}. Une étape qui engage un effet réversible sur le kernel (créer une commande
 * de vente, réserver du stock…) implémente cette interface en plus d'{@link ExecuteurDEtape} ; le
 * moteur la rappellera en ordre inverse si une étape ultérieure échoue.
 *
 * <p>La compensation est <i>best-effort</i> : elle reçoit le contexte <b>tel que l'étape l'a produit</b>
 * (donc avec les identifiants kernel qu'elle y a posés) et ne doit jamais masquer l'erreur d'origine.
 */
public interface EtapeCompensable extends ExecuteurDEtape {

    /** Annule l'effet engagé par {@link #executer}, à partir du contexte produit par cette étape. */
    Mono<Void> compenser(ContexteEtape contexte);
}
