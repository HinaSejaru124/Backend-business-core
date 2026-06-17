package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.businesstype.TypeMetier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — persistance des Types Métier.
 * L'adapter R2DBC implémente ce contrat. Le domaine ne connaît pas R2DBC.
 */
public interface PersisterTypeMetier {

    Mono<TypeMetier> sauvegarder(TypeMetier type);

    Mono<TypeMetier> trouverParId(UUID id);

    Mono<TypeMetier> trouverParCode(String code);

    Flux<TypeMetier> listerParTenant(UUID tenantId);

    Mono<Boolean> existeParCodeEtTenant(String code, UUID tenantId);
}
