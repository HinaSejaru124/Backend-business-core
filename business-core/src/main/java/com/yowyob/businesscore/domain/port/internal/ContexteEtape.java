package com.yowyob.businesscore.domain.port.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * État accumulé d'une opération à travers ses étapes. Chaque étape lit/écrit des données
 * (offre, quantité, transaction produite...) transmises à l'étape suivante.
 */
public record ContexteEtape(Map<String, Object> donnees) {

    public ContexteEtape {
        donnees = donnees == null ? Map.of() : Map.copyOf(donnees);
    }

    public static ContexteEtape vide() {
        return new ContexteEtape(Map.of());
    }

    public Object get(String cle) {
        return donnees.get(cle);
    }

    /** Retourne un nouveau contexte enrichi (immuable). */
    public ContexteEtape avec(String cle, Object valeur) {
        Map<String, Object> copie = new HashMap<>(donnees);
        copie.put(cle, valeur);
        return new ContexteEtape(copie);
    }
}
