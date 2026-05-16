package com.bcaas.core.audit.domain.model;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;

import java.time.Instant;
import java.util.Map;

/**
 * Entrée d'audit immuable.
 *
 * Enregistre chaque action significative dans le système BCaaS.
 * Une entrée d'audit ne peut jamais être modifiée ou supprimée.
 * Analogie réseau : log de pare-feu — enregistrement de chaque paquet.
 *
 * Contient :
 * - Qui  : actorId, tenantId
 * - Quoi : action, entityType, entityId
 * - Quand : occurredAt
 * - Comment : ipAddress, userAgent, traceId
 * - Résultat : success, errorMessage
 * - Détails : metadata (avant/après pour les updates)
 */
public record AuditEntry(
        AuditEntryId id,
        TenantId tenantId,
        ActorId actorId,
        AuditAction action,
        String entityType,
        String entityId,
        AuditSeverity severity,
        boolean success,
        String errorMessage,
        String ipAddress,
        String userAgent,
        String traceId,
        Map<String, String> metadata,
        Instant occurredAt
) {
    public AuditEntry {
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        if (occurredAt == null) throw new IllegalArgumentException("occurredAt est obligatoire");
        if (action == null) throw new IllegalArgumentException("action est obligatoire");
        if (entityType == null || entityType.isBlank())
            throw new IllegalArgumentException("entityType est obligatoire");
    }

    public static AuditEntry success(TenantId tenantId, ActorId actorId,
                                     AuditAction action, String entityType,
                                     String entityId, String traceId,
                                     Map<String, String> metadata) {
        return new AuditEntry(
                AuditEntryId.generate(), tenantId, actorId,
                action, entityType, entityId,
                resolveSeverity(action), true, null,
                null, null, traceId, metadata, Instant.now()
        );
    }

    public static AuditEntry failure(TenantId tenantId, ActorId actorId,
                                     AuditAction action, String entityType,
                                     String entityId, String errorMessage,
                                     String traceId) {
        return new AuditEntry(
                AuditEntryId.generate(), tenantId, actorId,
                action, entityType, entityId,
                AuditSeverity.WARNING, false, errorMessage,
                null, null, traceId, Map.of(), Instant.now()
        );
    }

    public static AuditEntry critical(TenantId tenantId, ActorId actorId,
                                      AuditAction action, String entityType,
                                      String entityId, String traceId) {
        return new AuditEntry(
                AuditEntryId.generate(), tenantId, actorId,
                action, entityType, entityId,
                AuditSeverity.CRITICAL, true, null,
                null, null, traceId, Map.of(), Instant.now()
        );
    }

    private static AuditSeverity resolveSeverity(AuditAction action) {
        return switch (action) {
            case DELETE, DEACTIVATE, PAYMENT_FAILED, WORKFLOW_FAILED -> AuditSeverity.WARNING;
            case LOGIN, LOGOUT, TOKEN_REFRESH -> AuditSeverity.INFO;
            default -> AuditSeverity.INFO;
        };
    }
}
