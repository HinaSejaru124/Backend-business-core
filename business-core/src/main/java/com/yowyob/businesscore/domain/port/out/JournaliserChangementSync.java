package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — journalise un changement de configuration métier d'une entreprise dans le journal
 * de synchronisation ({@code sync_event}), consommé en pull par les backends terminaux via
 * {@code GET /v1/sync}. Implémenté par le socle (persistance R2DBC).
 */
public interface JournaliserChangementSync {

    enum TypeEntiteSync {
        ENTREPRISE, ROLE, RULE, PARAMETRE, ACTEUR, OFFRE, OPERATION
    }

    enum OperationSync {
        CREATE, UPDATE, DELETE
    }

    /**
     * @param payload sérialisé en JSON par l'adapter (snapshot de l'entité au moment de l'événement)
     */
    Mono<Void> journaliser(UUID tenantId, UUID entrepriseId, TypeEntiteSync type, UUID entityId,
                           OperationSync operation, Object payload);
}
