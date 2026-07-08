package com.yowyob.businesscore.adapter.out.kernel.clientapp;

import com.yowyob.businesscore.domain.port.out.KernelClientCredentials;
import com.yowyob.businesscore.domain.port.out.ProvisionnerAccesDev;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Stub — le provisionnement kernel est une action admin.
 * Ce bean satisfait le port sans appeler le kernel.
 */
@Component
public class ProvisionnerAccesDevAdapter implements ProvisionnerAccesDev {

    @Override
    public Mono<KernelClientCredentials> provisionner(String planCode) {
        return Mono.error(new UnsupportedOperationException(
            "Le provisionnement kernel est une action admin — contacter Tsafack/Azangue"));
    }

    @Override
    public Mono<KernelClientCredentials> roterSecret(String kernelClientId) {
        return Mono.error(new UnsupportedOperationException(
            "La rotation de secret est une action admin"));
    }
}
