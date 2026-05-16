package com.bcaas.core.audit.application;

import com.bcaas.core.audit.domain.model.AuditAction;
import com.bcaas.core.audit.domain.model.AuditEntry;
import com.bcaas.core.audit.port.input.AuditUseCase;
import com.bcaas.core.audit.port.output.AuditRepository;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.Map;

/**
 * Service d'audit générique.
 * Enregistre chaque action significative de manière immuable.
 * Peut être appelé par n'importe quel service du Core.
 */
public class AuditService implements AuditUseCase {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Mono<AuditEntry> recordSuccess(AuditAction action, String entityType,
                                          String entityId, Map<String, String> metadata,
                                          BusinessContext context) {
        AuditEntry entry = AuditEntry.success(
                context.tenantId(), context.actorId(),
                action, entityType, entityId, context.traceId(), metadata
        );
        return auditRepository.save(entry);
    }

    @Override
    public Mono<AuditEntry> recordFailure(AuditAction action, String entityType,
                                          String entityId, String errorMessage,
                                          BusinessContext context) {
        AuditEntry entry = AuditEntry.failure(
                context.tenantId(), context.actorId(),
                action, entityType, entityId, errorMessage, context.traceId()
        );
        return auditRepository.save(entry);
    }

    @Override
    public Mono<AuditEntry> recordCritical(AuditAction action, String entityType,
                                           String entityId, BusinessContext context) {
        AuditEntry entry = AuditEntry.critical(
                context.tenantId(), context.actorId(),
                action, entityType, entityId, context.traceId()
        );
        return auditRepository.save(entry);
    }

    @Override
    public Flux<AuditEntry> getAuditTrail(TenantId tenantId, Instant from, Instant to) {
        return auditRepository.findByTenantId(tenantId, from, to);
    }

    @Override
    public Flux<AuditEntry> getEntityHistory(String entityType, String entityId) {
        return auditRepository.findByEntityId(entityType, entityId);
    }
}
