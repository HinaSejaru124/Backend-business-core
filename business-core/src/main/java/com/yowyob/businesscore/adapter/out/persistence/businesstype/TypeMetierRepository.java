package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository des types métier. Le filtrage par tenant est garanti par RLS (Barrière 3) : les
 * requêtes ne renvoient que les lignes du tenant courant. Dev 2 ajoute ses finders au besoin.
 */
public interface TypeMetierRepository extends ReactiveCrudRepository<TypeMetierEntity, UUID> {

    Mono<TypeMetierEntity> findByCode(String code);
}
