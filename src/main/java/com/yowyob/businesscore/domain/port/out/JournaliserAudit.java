package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Port de sortie — écrit une trace d'audit immuable.
 * Mappe : POST /api/audit. Implémenté par le socle.
 */
public interface JournaliserAudit {

    Mono<Void> journaliser(String action, String detail);
}
