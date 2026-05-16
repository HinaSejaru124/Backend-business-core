package com.bcaas.core.shared.domain;

import java.time.Instant;

/**
 * Value Object transversal porté par toutes les entités du domaine.
 * Capture qui a fait quoi et quand — immuable après création.
 */
public record AuditInfo(
        Instant createdAt,
        ActorId createdBy,
        Instant updatedAt,
        ActorId updatedBy
) {
    public static AuditInfo create(ActorId createdBy) {
        Instant now = Instant.now();
        return new AuditInfo(now, createdBy, now, createdBy);
    }

    public AuditInfo update(ActorId updatedBy) {
        return new AuditInfo(this.createdAt, this.createdBy, Instant.now(), updatedBy);
    }
}
