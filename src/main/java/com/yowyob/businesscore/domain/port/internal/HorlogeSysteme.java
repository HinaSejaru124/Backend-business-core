package com.yowyob.businesscore.domain.port.internal;

import java.time.Instant;

/**
 * Port interne (stratégie) — abstrait le temps pour la testabilité.
 * Implémenté par le socle (horloge système) ; les tests peuvent fournir une horloge fixe.
 */
public interface HorlogeSysteme {

    Instant maintenant();
}
