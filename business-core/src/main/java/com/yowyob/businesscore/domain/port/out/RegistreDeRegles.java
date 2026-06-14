package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.shared.Declencheur;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Port de sortie — charge les règles applicables (Type + Entreprise) pour un déclencheur.
 * Implémenté côté persistence par la brique Règles (Dev 4).
 */
public interface RegistreDeRegles {

    Flux<RegleChargee> chargerPourDeclencheur(UUID entrepriseId, Declencheur declencheur);
}
