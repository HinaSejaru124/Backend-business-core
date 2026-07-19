package com.yowyob.businesscore.domain.enterprise.spi;

import com.yowyob.businesscore.domain.enterprise.EntrepriseContrat;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — persistance du contrat technique d'une entreprise (1:1). Le filtrage tenant est
 * garanti par la RLS.
 */
public interface DepotEntrepriseContrat {

    Mono<EntrepriseContrat> sauvegarder(EntrepriseContrat contrat);

    Mono<EntrepriseContrat> trouverParEntreprise(UUID entrepriseId);
}
