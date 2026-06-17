package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.businesstype.VersionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — persistance des Versions de Type Métier.
 */
public interface PersisterVersionType {

    Mono<VersionType> sauvegarder(VersionType version);

    Mono<VersionType> trouverParId(UUID id);

    Flux<VersionType> listerParTypeMetier(UUID typeMetierId);

    /** Retourne le numéro de la dernière version, 0 si aucune version. */
    Mono<Integer> dernierNumero(UUID typeMetierId);
}
