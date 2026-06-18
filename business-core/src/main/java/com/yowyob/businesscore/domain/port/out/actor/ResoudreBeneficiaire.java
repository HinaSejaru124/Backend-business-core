package com.yowyob.businesscore.domain.port.out.actor;

import reactor.core.publisher.Mono;

import java.util.UUID;

/** Bénéficiaire (externe). Impl : POST /api/third-parties. Renvoie le tiersId kernel. */
public interface ResoudreBeneficiaire {
    Mono<UUID> resoudreBeneficiaire(String identifiant);
}
