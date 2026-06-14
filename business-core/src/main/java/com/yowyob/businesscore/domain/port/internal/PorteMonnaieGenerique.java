package com.yowyob.businesscore.domain.port.internal;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port interne (stratégie) — réalise un échange de valeur générique.
 * Le monétaire est l'implémentation de départ ; troc/don pourront suivre sans toucher au domaine.
 */
public interface PorteMonnaieGenerique {

    Mono<UUID> enregistrerEchange(BigDecimal montant, String devise);
}
