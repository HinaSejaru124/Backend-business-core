package com.yowyob.businesscore.adapter.out.kernel.product;

import com.yowyob.businesscore.domain.port.out.offer.VerifierDisponibilite;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class DisponibiliteKernelAdapter implements VerifierDisponibilite {

    private final KernelClient kernel;

    public DisponibiliteKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record Balance(BigDecimal balance) {}

    @Override
    public Mono<BigDecimal> soldeDisponible(UUID productId) {
        return kernel.get("/api/inventory/movements/balance?productId=" + productId, Balance.class)
                .map(Balance::balance);
    }
}