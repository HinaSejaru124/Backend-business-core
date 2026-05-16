package com.bcaas.core.shared.domain;

import java.util.UUID;

/**
 * Value Object représentant l'identifiant unique d'un acteur.
 */
public record ActorId(UUID value) {

    public ActorId {
        if (value == null) {
            throw new IllegalArgumentException("ActorId ne peut pas être null");
        }
    }

    public static ActorId generate() {
        return new ActorId(UUID.randomUUID());
    }

    public static ActorId of(String value) {
        return new ActorId(UUID.fromString(value));
    }

    public static ActorId of(UUID value) {
        return new ActorId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
