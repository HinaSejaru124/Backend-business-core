package com.yowyob.businesscore.domain.port.out.enterprise;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port de persistance locale de l'Entreprise (implémenté par l'adapter R2DBC). */
public interface DepotEntreprise {
    Mono<Entreprise> enregistrer(Entreprise entreprise);
    Mono<Entreprise> parId(UUID id);
    Flux<Entreprise> toutes();
}
