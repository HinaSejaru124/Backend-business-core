package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.shared.Declencheur;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Port de sortie — charge les règles applicables pour un déclencheur : les règles de Type de la
 * {@code versionTypeId} ciblée <b>plus</b> les règles locales de l'{@code entrepriseId} ciblée.
 * Implémenté côté persistence par la brique Règles (Dev 4).
 */
public interface RegistreDeRegles {

    Flux<RegleChargee> chargerPourDeclencheur(UUID entrepriseId, UUID versionTypeId, Declencheur declencheur);
}
