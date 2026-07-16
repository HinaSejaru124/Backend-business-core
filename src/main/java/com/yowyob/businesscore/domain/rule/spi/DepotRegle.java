package com.yowyob.businesscore.domain.rule.spi;

import com.yowyob.businesscore.domain.rule.RegleMetier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port de persistance des règles métier (Type ou locales). */
public interface DepotRegle {
    Mono<RegleMetier> sauvegarder(RegleMetier regle);
    Mono<RegleMetier> trouverParId(UUID id);
    Flux<RegleMetier> listerParVersionType(UUID versionTypeId);
    Flux<RegleMetier> listerParEntreprise(UUID entrepriseId);
    Mono<Void> supprimer(UUID id);
}
