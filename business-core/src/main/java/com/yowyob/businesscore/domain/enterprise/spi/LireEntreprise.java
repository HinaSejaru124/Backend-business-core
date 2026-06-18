package com.yowyob.businesscore.domain.enterprise.spi;

import com.yowyob.businesscore.domain.enterprise.Entreprise;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port de LECTURE de l'Entreprise, exposé volontairement aux autres features
 * (Dev 2 = surcharge de configuration, etc.) afin qu'ils n'aient pas à dépendre
 * de l'adapter de persistance. Forme figée tôt — préviens l'équipe avant tout changement.
 */
public interface LireEntreprise {
    Mono<Entreprise> parId(UUID id);
}
