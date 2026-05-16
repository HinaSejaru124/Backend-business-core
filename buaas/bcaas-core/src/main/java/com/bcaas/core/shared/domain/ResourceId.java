package com.bcaas.core.shared.domain;

import java.util.UUID;

/**
 * Value Object représentant l'identifiant unique d'une ressource métier.
 */
public record ResourceId(UUID value) {

    public ResourceId {
        if (value == null) {
            throw new IllegalArgumentException("ResourceId ne peut pas être null");
        }
    }

    public static ResourceId generate() {
        return new ResourceId(UUID.randomUUID());
    }

    public static ResourceId of(String value) {
        return new ResourceId(UUID.fromString(value));
    }

    public static ResourceId of(UUID value) {
        return new ResourceId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
