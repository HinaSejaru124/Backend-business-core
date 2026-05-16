package com.bcaas.core.audit.port.output;

import com.bcaas.core.audit.domain.model.AuditAction;
import com.bcaas.core.audit.domain.model.AuditEntry;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;

public interface AuditRepository {
    Mono<AuditEntry> save(AuditEntry entry);
    Flux<AuditEntry> findByTenantId(TenantId tenantId, Instant from, Instant to);
    Flux<AuditEntry> findByActorId(ActorId actorId, Instant from, Instant to);
    Flux<AuditEntry> findByEntityId(String entityType, String entityId);
    Flux<AuditEntry> findByAction(TenantId tenantId, AuditAction action);
}
