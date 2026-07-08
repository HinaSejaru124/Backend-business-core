package com.yowyob.businesscore.adapter.out.persistence.product;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.domain.offer.spi.DepotProduitEntreprise;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Adapter R2DBC du port {@link DepotProduitEntreprise} (table {@code produit_entreprise}, RLS par tenant). */
@Component
public class ProduitEntreprisePersistenceAdapter implements DepotProduitEntreprise {

    private final ProduitEntrepriseRepository repository;

    public ProduitEntreprisePersistenceAdapter(ProduitEntrepriseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<UUID> trouverProductId(UUID entrepriseId, UUID offreId) {
        return repository.findByEntrepriseIdAndOffreId(entrepriseId, offreId)
                .map(entity -> entity.getProductId());
    }

    @Override
    public Mono<Void> enregistrer(UUID entrepriseId, UUID offreId, UUID productId) {
        return BusinessContextHolder.currentContext().flatMap(ctx -> repository.save(
                        ProduitEntrepriseEntity.nouveau(
                                UUID.randomUUID(), ctx.tenantId(), entrepriseId, offreId, productId))
                .then());
    }
}
