package com.yowyob.businesscore.domain.port.out;

import com.yowyob.businesscore.domain.shared.Declencheur;
import com.yowyob.businesscore.domain.shared.Effet;

import java.util.List;
import java.util.UUID;

/**
 * Vue d'une règle applicable telle que chargée par le registre, indépendante du modèle interne de
 * la brique Règles. Contrat minimal entre {@code RegistreDeRegles} et {@code EvaluateurDeRegle}.
 */
public record RegleChargee(
        UUID id,
        Declencheur declencheur,
        String condition,
        Effet effet,
        List<String> rolesAutorisesADeroger,
        boolean porteeType
) {
    public RegleChargee {
        rolesAutorisesADeroger = rolesAutorisesADeroger == null ? List.of() : List.copyOf(rolesAutorisesADeroger);
    }
}
