package com.bcaas.core.actor.domain.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Profil extensible d'un acteur.
 * Le Business Core ne connaît pas les détails métier spécifiques.
 * Les applications (BuaaS, transport) enrichissent ce profil via
 * les metadata (clé-valeur générique).
 *
 * BuaaS ajoute : specialization, yearsOfExperience, linkedinUrl
 * Transport ajoute : licenseNumber, vehicleType, rating
 */
public record ActorProfile(
        String bio,
        String avatarUrl,
        String location,
        Map<String, String> metadata
) {
    public ActorProfile {
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static ActorProfile empty() {
        return new ActorProfile(null, null, null, Map.of());
    }

    public ActorProfile withMetadata(String key, String value) {
        Map<String, String> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new ActorProfile(bio, avatarUrl, location, newMetadata);
    }

    public String getMetadata(String key) {
        return metadata.getOrDefault(key, null);
    }
}
