package com.yowyob.businesscore.adapter.out.kernel.product;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.GererCatalogueOffre;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduit notre Offre (plus riche) vers le Product kernel :
 * - STOCKABLE -> produit avec gestion de stock (trackInventory=true),
 * - prix absent -> produit sans prix fixe (priceless=true, ex. SUR_DEVIS / GRATUIT).
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
    public Mono<UUID> creerProduit(String nom, boolean stockable, BigDecimal prix) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", nom);
        payload.put("trackInventory", stockable);
        payload.put("priceless", prix == null);
        if (prix != null) {
            payload.put("price", prix);
        }
        return kernel.post("/api/products", payload, KernelId.class).map(KernelId::id);
    }

    @Override
    public Mono<BigDecimal> prixEffectif(UUID produitId) {
        return kernel.get("/api/products/" + produitId + "/prices/effective", PrixEffectif.class)
                .map(PrixEffectif::amount);
    }
}
