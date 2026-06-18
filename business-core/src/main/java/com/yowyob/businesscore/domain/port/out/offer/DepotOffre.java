package com.yowyob.businesscore.domain.port.out.offer;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port de persistance locale des offres + capacités (adapter R2DBC). */
public interface DepotOffre {
    /** Persiste la définition et ses capacités, renvoie l'agrégat rechargé. */
    Mono<DefinitionOffre> enregistrer(DefinitionOffre offre);
    Flux<DefinitionOffre> parVersionType(UUID versionTypeId);
    Mono<DefinitionOffre> parId(UUID id);
}
