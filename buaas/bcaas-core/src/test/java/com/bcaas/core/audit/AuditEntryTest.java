package com.bcaas.core.audit;

import com.bcaas.core.audit.domain.model.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AuditEntry — traçabilité immuable")
class AuditEntryTest {

    private final TenantId tenantId = TenantId.generate();
    private final ActorId actorId = ActorId.generate();

    @Test
    @DisplayName("Une entrée de succès est correctement construite")
    void shouldCreateSuccessEntry() {
        AuditEntry entry = AuditEntry.success(
                tenantId, actorId, AuditAction.CREATE,
                "Resource", "resource-123", "trace-xyz",
                Map.of("title", "Ingénieur")
        );

        assertThat(entry.success()).isTrue();
        assertThat(entry.action()).isEqualTo(AuditAction.CREATE);
        assertThat(entry.entityType()).isEqualTo("Resource");
        assertThat(entry.severity()).isEqualTo(AuditSeverity.INFO);
        assertThat(entry.occurredAt()).isNotNull();
        assertThat(entry.id()).isNotNull();
    }

    @Test
    @DisplayName("Une entrée d'échec contient le message d'erreur")
    void shouldCreateFailureEntry() {
        AuditEntry entry = AuditEntry.failure(
                tenantId, actorId, AuditAction.DELETE,
                "Tenant", "tenant-456", "Permission refusée", "trace-abc"
        );

        assertThat(entry.success()).isFalse();
        assertThat(entry.errorMessage()).isEqualTo("Permission refusée");
        assertThat(entry.severity()).isEqualTo(AuditSeverity.WARNING);
    }

    @Test
    @DisplayName("Une entrée critique a le bon niveau de sévérité")
    void shouldCreateCriticalEntry() {
        AuditEntry entry = AuditEntry.critical(
                tenantId, actorId, AuditAction.DEACTIVATE,
                "Actor", "actor-789", "trace-critical"
        );

        assertThat(entry.severity()).isEqualTo(AuditSeverity.CRITICAL);
        assertThat(entry.success()).isTrue();
    }

    @Test
    @DisplayName("Les metadata sont immuables")
    void shouldBeImmutable() {
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("key", "value");

        AuditEntry entry = AuditEntry.success(
                tenantId, actorId, AuditAction.UPDATE,
                "Resource", "r-1", "trace", metadata
        );

        metadata.put("newKey", "newValue");

        assertThat(entry.metadata()).doesNotContainKey("newKey");
    }

    @Test
    @DisplayName("Une entrée sans entityType est rejetée")
    void shouldRejectMissingEntityType() {
        assertThatThrownBy(() ->
            AuditEntry.success(tenantId, actorId, AuditAction.CREATE,
                    "", "id", "trace", Map.of())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
