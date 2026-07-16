package com.yowyob.businesscore.adapter.out.persistence.sync;

import com.yowyob.businesscore.domain.port.out.JournaliserChangementSync;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Implémentation socle : sérialise le payload en JSON et insère la ligne {@code sync_event}.
 * Append-only, pas de transaction multi-tables nécessaire (le curseur est le {@code id} bigserial,
 * intrinsèquement monotone — pas de compteur séparé à synchroniser).
 */
@Component
public class JournaliserChangementSyncAdapter implements JournaliserChangementSync {

    private final SyncEventRepository repository;
    private final ObjectMapper objectMapper;

    public JournaliserChangementSyncAdapter(SyncEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> journaliser(UUID tenantId, UUID entrepriseId, TypeEntiteSync type, UUID entityId,
                                  OperationSync operation, Object payload) {
        return Mono.fromCallable(() -> payload == null ? null : objectMapper.writeValueAsString(payload))
                .flatMap(json -> repository.save(SyncEventEntity.nouveau(
                        tenantId, entrepriseId, type.name(), entityId, operation.name(), json)))
                .then();
    }
}
