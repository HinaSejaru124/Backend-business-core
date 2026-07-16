package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Port de sortie — provisionne l'accès kernel d'un développeur.
 * Implémenté par le socle (infra partagée).
 */
public interface ProvisionnerAccesDev {

    Mono<KernelClientCredentials> provisionner(String planCode);

    Mono<KernelClientCredentials> roterSecret(String kernelClientId);
}
