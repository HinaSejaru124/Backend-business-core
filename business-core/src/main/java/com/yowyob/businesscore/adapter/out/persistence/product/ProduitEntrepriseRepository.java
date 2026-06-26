package com.yowyob.businesscore.adapter.out.persistence.product;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProduitEntrepriseRepository extends ReactiveCrudRepository<ProduitEntrepriseEntity, UUID> {

    Mono<ProduitEntrepriseEntity> findByEntrepriseIdAndOffreId(UUID entrepriseId, UUID offreId);
}
