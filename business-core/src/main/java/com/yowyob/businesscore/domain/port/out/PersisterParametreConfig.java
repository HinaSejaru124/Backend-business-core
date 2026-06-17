package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.configuration.ParametreConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — persistance des paramètres de configuration.
 */
public interface PersisterParametreConfig {

    Mono<ParametreConfig> sauvegarder(ParametreConfig parametre);

    /** Trouve un paramètre par clé au niveau d'une VersionType. */
    Mono<ParametreConfig> trouverParCleEtVersion(String cle, UUID versionTypeId);

    /** Trouve un paramètre par clé au niveau d'une Entreprise. */
    Mono<ParametreConfig> trouverParCleEtEntreprise(String cle, UUID entrepriseId);

    /** Liste tous les paramètres d'une VersionType. */
    Flux<ParametreConfig> listerParVersion(UUID versionTypeId);

    /** Liste tous les paramètres d'une Entreprise. */
    Flux<ParametreConfig> listerParEntreprise(UUID entrepriseId);

    Mono<Void> supprimer(UUID id);
}
