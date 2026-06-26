package com.yowyob.businesscore.adapter.out.kernel.inventory;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.out.VerifierDisponibilite;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter kernel — implémente {@link VerifierDisponibilite}. Lit le solde de stock d'un produit sur le
 * core inventory du kernel ({@code GET /api/inventory/movements/balance}). Le kernel exige trois query
 * params : {@code organizationId}, {@code agencyId}, {@code productId}. Les deux premiers sont résolus
 * via le {@link ResolveurContexteKernel} à partir du {@code businessId} — le métier ne les fournit pas.
 */
@Component
public class VerifierDisponibiliteKernelAdapter implements VerifierDisponibilite {

    private final KernelClient kernel;
    private final ResolveurContexteKernel resolveur;

    public VerifierDisponibiliteKernelAdapter(KernelClient kernel, ResolveurContexteKernel resolveur) {
        this.kernel = kernel;
        this.resolveur = resolveur;
    }

    @Override
    public Mono<Long> soldeStock(UUID productId, UUID businessId) {
        return resolveur.resoudre(businessId).flatMap(ctx -> kernel.getForOrganization(
                        "/api/inventory/movements/balance?organizationId=" + ctx.organizationId()
                                + "&agencyId=" + ctx.agencyId() + "&productId=" + productId,
                        SoldeResponse.class, ctx.organizationId())
                .map(SoldeResponse::balance));
    }
}

record SoldeResponse(long balance) {
}
