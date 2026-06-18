package com.yowyob.businesscore.adapter.out.kernel.product;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.port.out.offer.GererCatalogueOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Traduit notre Offre (plus riche) vers le Product kernel :
 * - STOCKABLE -> produit avec gestion de stock (trackInventory=true),
 * - SUR_DEVIS -> produit sans prix fixe (priceless=true), etc.
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
    public Mono<UUID> publierCommeProduit(DefinitionOffre offre) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", offre.nom());
        payload.put("trackInventory", offre.possede(TypeCapacite.STOCKABLE));
        payload.put("priceless", offre.formePrix() == FormePrix.SUR_DEVIS);
        if (offre.formePrix() == FormePrix.FIXE) {
            payload.put("price", offre.prix());
        }
        return kernel.post("/api/products", payload, KernelId.class).map(KernelId::id);
    }

    @Override
    public Mono<BigDecimal> prixEffectif(UUID productId) {
        return kernel.get("/api/products/" + productId + "/prices/effective", PrixEffectif.class)
                .map(PrixEffectif::amount);
    }
}