package com.yowyob.businesscore.adapter.out.kernel.product;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.CreationProduit;
import com.yowyob.businesscore.domain.port.out.GererCatalogueOffre;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduit notre Offre (plus riche) vers le Product kernel ({@code POST /api/products}).
 * Le Product kernel est scopé organisation : tous les champs obligatoires de {@code CreateProductRequest}
 * ({@code organizationId}, {@code sku}, {@code name}, {@code familyCode}, {@code variantLabel},
 * {@code unitPrice}, {@code currency}) sont fournis par le socle via {@link CreationProduit}.
 * - STOCKABLE -> {@code trackInventory=true} ; sans prix fixe -> {@code priceless=true} (SUR_DEVIS / GRATUIT).
 */
@Component
public class CatalogueOffreKernelAdapter implements GererCatalogueOffre {

    private final KernelClient kernel;

    public CatalogueOffreKernelAdapter(KernelClient kernel) {
        this.kernel = kernel;
    }

    record KernelId(UUID id) {}
    record PrixEffectif(BigDecimal amount) {}

    @Override
    public Mono<UUID> creerProduit(CreationProduit demande) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", demande.organizationId());
        payload.put("sku", demande.sku());
        payload.put("name", demande.nom());
        payload.put("familyCode", demande.familyCode());
        payload.put("variantLabel", demande.variantLabel());
        payload.put("unitPrice", demande.unitPrice());
        payload.put("currency", demande.currency());
        payload.put("trackInventory", demande.stockable());
        payload.put("priceless", demande.priceless());
        return kernel.postForOrganization("/api/products", payload, KernelId.class, demande.organizationId())
                .map(KernelId::id);
    }

    @Override
    public Mono<BigDecimal> prixEffectif(UUID produitId) {
        return kernel.get("/api/products/" + produitId + "/prices/effective", PrixEffectif.class)
                .map(PrixEffectif::amount);
    }
}
