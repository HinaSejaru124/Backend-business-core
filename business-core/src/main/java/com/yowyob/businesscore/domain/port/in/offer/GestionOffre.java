package com.yowyob.businesscore.domain.port.in.offer;

import com.yowyob.businesscore.domain.offer.DefinitionOffre;
import com.yowyob.businesscore.domain.shared.FormePrix;
import com.yowyob.businesscore.domain.shared.TypeCapacite;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Port d'entrée (use cases) de l'Offre. */
public interface GestionOffre {

    record DeclarerOffreCommande(
            UUID versionTypeId,
            String nom,
            FormePrix formePrix,
            BigDecimal prix,
            Set<TypeCapacite> capacites) {}

    Mono<DefinitionOffre> declarer(DeclarerOffreCommande commande);

    Flux<DefinitionOffre> lister(UUID versionTypeId);
}
