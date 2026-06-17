package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Repository des versions de type métier. RLS garantit l'isolation tenant. */
public interface VersionTypeRepository
        extends ReactiveCrudRepository<VersionTypeEntity, UUID> {

    Flux<VersionTypeEntity> findAllByTypeMetierId(UUID typeMetierId);

    /** Résolution d'une version par (type, numéro) — adressage REST par numéro + brique Règles. */
    Mono<VersionTypeEntity> findByTypeMetierIdAndNumero(UUID typeMetierId, int numero);

    /** Dernier numéro de version d'un type (0 si aucune) — calcul atomique du prochain numéro. */
    @Query("SELECT COALESCE(MAX(numero), 0) FROM version_type WHERE type_metier_id = :typeMetierId")
    Mono<Integer> findMaxNumero(@Param("typeMetierId") UUID typeMetierId);
}
