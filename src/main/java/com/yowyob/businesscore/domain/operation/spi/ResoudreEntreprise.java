package com.yowyob.businesscore.domain.operation.spi;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port (feature Opérations) — résout une entreprise ciblée en {@link EntrepriseResolue}.
 *
 * <p>C'est l'unique point de contact entre la brique Opérations et la brique Entreprise (Dev 3) :
 * passer par ce port évite toute dépendance directe entre features. Implémenté par l'adapter de
 * persistance Entreprise (fourni en version minimale ici, à compléter par Dev 3).
 */
public interface ResoudreEntreprise {

    Mono<EntrepriseResolue> resoudre(UUID entrepriseId);
}
