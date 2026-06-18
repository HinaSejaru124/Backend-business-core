package com.yowyob.businesscore.domain.port.out.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port de persistance locale des rôles métier et acteurs métier (adapter R2DBC). */
public interface DepotActeur {
    Mono<RoleMetier> enregistrerRole(RoleMetier role);
    Mono<RoleMetier> roleParId(UUID id);

    Mono<ActeurMetier> enregistrerActeur(ActeurMetier acteur);
    Mono<ActeurMetier> acteurParId(UUID id);
    Flux<ActeurMetier> acteursParEntreprise(UUID entrepriseId);
}
