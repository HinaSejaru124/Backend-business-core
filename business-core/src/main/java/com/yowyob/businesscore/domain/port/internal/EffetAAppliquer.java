package com.yowyob.businesscore.domain.port.internal;

import com.yowyob.businesscore.domain.shared.Effet;

import java.util.Map;
import java.util.UUID;

/**
 * Effet retourné par l'évaluateur pour une règle déclenchée.
 * Pour AJUSTER, {@code details} porte la valeur d'origine et la valeur corrigée (jamais silencieux).
 */
public record EffetAAppliquer(Effet effet, UUID regleId, String message, Map<String, Object> details) {

    public EffetAAppliquer {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
