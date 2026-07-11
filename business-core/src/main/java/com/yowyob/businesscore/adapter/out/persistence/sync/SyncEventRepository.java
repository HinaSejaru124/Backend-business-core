package com.yowyob.businesscore.adapter.out.persistence.sync;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SyncEventRepository extends ReactiveCrudRepository<SyncEventEntity, Long> {

    Flux<SyncEventEntity> findByEntrepriseIdAndIdGreaterThanOrderByIdAsc(UUID entrepriseId, Long since,
                                                                         Pageable page);

    Mono<SyncEventEntity> findFirstByEntrepriseIdOrderByIdDesc(UUID entrepriseId);
}
