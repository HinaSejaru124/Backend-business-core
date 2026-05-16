package com.bcaas.core.audit.domain.model;

import java.util.UUID;

public record AuditEntryId(UUID value) {
    public AuditEntryId {
        if (value == null) throw new IllegalArgumentException("AuditEntryId ne peut pas être null");
    }
    public static AuditEntryId generate() { return new AuditEntryId(UUID.randomUUID()); }
    public static AuditEntryId of(String value) { return new AuditEntryId(UUID.fromString(value)); }
    @Override public String toString() { return value.toString(); }
}
