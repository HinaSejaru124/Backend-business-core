package com.yowyob.businesscore.adapter.out.kernel.inventory;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.ResolveurContexteKernel;
import com.yowyob.businesscore.domain.port.out.EngagerStock;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter kernel — crée et valide un mouvement de stock OUT lié à une vente, ou un mouvement IN
 * compensatoire en cas d'annulation.
 */
@Component
public class EngagerStockKernelAdapter implements EngagerStock {

    private static final String TYPE_SORTIE = "OUT";
    private static final String TYPE_ENTREE = "IN";
    private static final String SOURCE_VENTE = "SALES_ORDER";

    private final KernelClient kernel;
    private final ResolveurContexteKernel resolveur;

    public EngagerStockKernelAdapter(KernelClient kernel, ResolveurContexteKernel resolveur) {
        this.kernel = kernel;
        this.resolveur = resolveur;
    }

    @Override
    public Mono<UUID> sortieVente(UUID productId, int quantite, UUID businessId, UUID commandeId) {
        return resolveur.resoudre(businessId).flatMap(ctx ->
                creerEtValider(ctx.organizationId(), ctx.agencyId(), productId, quantite,
                        TYPE_SORTIE, commandeId));
    }

    @Override
    public Mono<Void> annulerSortie(UUID productId, int quantite, UUID businessId, UUID commandeId) {
        return resolveur.resoudre(businessId).flatMap(ctx ->
                creerEtValider(ctx.organizationId(), ctx.agencyId(), productId, quantite,
                        TYPE_ENTREE, commandeId).then());
    }

    private Mono<UUID> creerEtValider(UUID organizationId, UUID agencyId, UUID productId,
                                      int quantite, String movementType, UUID commandeId) {
        if (agencyId == null) {
            return Mono.error(new IllegalStateException("Agence kernel requise pour engager le stock."));
        }
        Map<String, Object> corps = Map.of(
                "organizationId", organizationId,
                "agencyId", agencyId,
                "productId", productId,
                "movementType", movementType,
                "quantity", BigDecimal.valueOf(quantite),
                "sourceDocumentType", SOURCE_VENTE,
                "referenceNumber", commandeId != null ? commandeId.toString() : "");
        return kernel.postForOrganization("/api/inventory/movements", corps, MouvementResponse.class,
                        organizationId)
                .flatMap(mouvement -> kernel.postForOrganization(
                        "/api/inventory/movements/" + mouvement.id() + "/validate",
                        null, Void.class, organizationId)
                        .thenReturn(mouvement.id()));
    }
}

record MouvementResponse(UUID id) {
}
