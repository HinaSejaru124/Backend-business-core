package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — référence la taxonomie kernel des domaines métier.
 * Mappe : GET/POST /api/business-domains.
 */
public interface ResoudreBusinessDomain {

    Mono<UUID> resoudreOuCreer(String code, String nom);

    Flux<BusinessDomainRef> lister();
}
