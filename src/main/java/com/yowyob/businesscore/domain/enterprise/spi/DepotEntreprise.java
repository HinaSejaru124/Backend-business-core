package com.yowyob.businesscore.domain.enterprise.spi;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie (brique Entreprise, version minimale) — persistance locale des entreprises.
 * Le filtrage tenant est garanti par la RLS. À compléter/fusionner avec le travail de Dev 3.
 */
public interface DepotEntreprise {

    Mono<Entreprise> sauvegarder(Entreprise entreprise);

    Mono<Entreprise> trouverParId(UUID id);

    Flux<Entreprise> listerParTenant(UUID tenantId);
}
