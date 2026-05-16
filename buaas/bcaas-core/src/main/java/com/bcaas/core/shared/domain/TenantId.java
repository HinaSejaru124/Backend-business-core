package com.bcaas.core.shared.domain;

import java.util.UUID;

/**
 * Value Object représentant l'identifiant unique d'un tenant.
 * Immutable par construction — deux TenantId avec le même UUID sont égaux.
 */
public record TenantId(UUID value) {

    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("TenantId ne peut pas être null");
        }
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    public static TenantId of(String value) {
        return new TenantId(UUID.fromString(value));
    }

    public static TenantId of(UUID value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
