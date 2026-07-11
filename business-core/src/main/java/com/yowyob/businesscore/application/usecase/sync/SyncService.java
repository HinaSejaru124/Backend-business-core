package com.yowyob.businesscore.application.usecase.sync;

import com.yowyob.businesscore.adapter.out.persistence.sync.SyncEventEntity;
import com.yowyob.businesscore.adapter.out.persistence.sync.SyncEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Synchronisation pull pour les backends terminaux (mode offline).
 *
 * <p>Le journal {@code sync_event} est un journal d'incréments purs, jamais purgé (v1) : le
 * "snapshot initial" ({@code since=0}) est donc simplement le rejeu complet du journal depuis le
 * début, pas une agrégation séparée des tables courantes — plus simple, toujours cohérent avec l'état
 * réel tant qu'aucune purge n'est introduite (cf. limite documentée : à revisiter si la volumétrie du
 * journal devient un problème).
 */
@Service
public class SyncService {

    private static final int LIMITE_MAX = 500;

    private final SyncEventRepository repository;

    public SyncService(SyncEventRepository repository) {
        this.repository = repository;
    }

    public record SyncEventItem(long version, String entityType, UUID entityId, String operation, String payload) {
        static SyncEventItem depuis(SyncEventEntity e) {
            return new SyncEventItem(e.getId(), e.getEntityType(), e.getEntityId(), e.getOperation(), e.getPayload());
        }
    }

    public record SyncResultat(long versionCourante, List<SyncEventItem> items) {
    }

    public Mono<SyncResultat> consulter(UUID entrepriseId, long since, Integer limit) {
        int page = Math.min(limit == null || limit <= 0 ? 100 : limit, LIMITE_MAX);
        return repository.findByEntrepriseIdAndIdGreaterThanOrderByIdAsc(
                        entrepriseId, since, PageRequest.of(0, page))
                .map(SyncEventItem::depuis)
                .collectList()
                .flatMap(items -> repository.findFirstByEntrepriseIdOrderByIdDesc(entrepriseId)
                        .map(SyncEventEntity::getId)
                        .defaultIfEmpty(since)
                        .map(versionCourante -> new SyncResultat(versionCourante, items)));
    }
}
