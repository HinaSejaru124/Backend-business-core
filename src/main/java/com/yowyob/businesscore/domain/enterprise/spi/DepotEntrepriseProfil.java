package com.yowyob.businesscore.domain.enterprise.spi;

import com.yowyob.businesscore.domain.enterprise.EntrepriseProfil;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de sortie — persistance de la fiche produit d'une entreprise (1:1). Le filtrage tenant est
 * garanti par la RLS.
 */
public interface DepotEntrepriseProfil {

    Mono<EntrepriseProfil> sauvegarder(EntrepriseProfil profil);

    Mono<EntrepriseProfil> trouverParEntreprise(UUID entrepriseId);
}
