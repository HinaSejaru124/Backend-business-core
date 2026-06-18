package com.yowyob.businesscore.domain.port.in.enterprise;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import com.yowyob.businesscore.domain.shared.CycleVie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Port d'entrée (use cases) de l'Entreprise. */
public interface GestionEntreprise {

    record CreerEntrepriseCommande(UUID versionTypeId, String nomLocal) {}

    Mono<Entreprise> creer(CreerEntrepriseCommande commande);

    Flux<Entreprise> lister();

    Mono<Entreprise> consulter(UUID businessId);

    Mono<Entreprise> changerCycleVie(UUID businessId, CycleVie cible);
}
