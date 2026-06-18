package com.yowyob.businesscore.adapter.out.kernel.inventory;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.VerifierDisponibilite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter kernel — implémente {@link VerifierDisponibilite}. Lit le solde de stock d'un produit sur
 * le core inventory du kernel ({@code GET /api/inventory/movements/balance}) via {@link KernelClient}.
 */
@Component
public class VerifierDisponibiliteKernelAdapter implements VerifierDisponibilite {

    private final KernelClient kernel;

    public VerifierDisponibiliteKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    @Override
    public Mono<Long> soldeStock(UUID produitId) {
        return kernel.get("/api/inventory/movements/balance?productId=" + produitId, SoldeResponse.class)
                .map(SoldeResponse::balance);
    }
}

record SoldeResponse(long balance) {
}
