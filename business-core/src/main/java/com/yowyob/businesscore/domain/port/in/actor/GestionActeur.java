package com.yowyob.businesscore.domain.port.in.actor;

import com.yowyob.businesscore.domain.actor.ActeurMetier;
import com.yowyob.businesscore.domain.actor.RoleMetier;
import com.yowyob.businesscore.domain.shared.CategorieActeur;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port d'entrée (use cases) des Acteurs métier. */
public interface GestionActeur {

    record DeclarerRoleCommande(UUID versionTypeId, String code, CategorieActeur categorie) {}

    record RattacherActeurCommande(UUID businessId, UUID roleMetierId, String identifiantPersonne) {}

    Mono<RoleMetier> declarerRole(DeclarerRoleCommande commande);

    Mono<ActeurMetier> rattacher(RattacherActeurCommande commande);

    Flux<ActeurMetier> lister(UUID businessId);

    Mono<Void> detacher(UUID businessId, UUID actorId);
}
