package com.yowyob.businesscore.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Port de sortie — provisionne l'accès kernel d'un développeur.
 * Mappe : POST /api/client-applications ; POST /api/client-applications/{id}/rotate-secret.
 * Implémenté par le socle (infra partagée).
 */
public interface ProvisionnerAccesDev {

    Mono<KernelClientCredentials> provisionner(String planCode);

    Mono<KernelClientCredentials> roterSecret(String kernelClientId);
}
