package com.bcaas.core.audit.port.input;

import com.bcaas.core.audit.domain.model.AuditAction;
import com.bcaas.core.audit.domain.model.AuditEntry;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.Map;

public interface AuditUseCase {
    Mono<AuditEntry> recordSuccess(AuditAction action, String entityType,
                                   String entityId, Map<String, String> metadata,
                                   BusinessContext context);
    Mono<AuditEntry> recordFailure(AuditAction action, String entityType,
                                   String entityId, String errorMessage,
                                   BusinessContext context);
    Mono<AuditEntry> recordCritical(AuditAction action, String entityType,
                                    String entityId, BusinessContext context);
    Flux<AuditEntry> getAuditTrail(TenantId tenantId, Instant from, Instant to);
    Flux<AuditEntry> getEntityHistory(String entityType, String entityId);
}
