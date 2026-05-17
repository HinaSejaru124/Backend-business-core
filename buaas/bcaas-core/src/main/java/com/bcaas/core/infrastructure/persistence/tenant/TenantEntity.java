package com.bcaas.core.infrastructure.persistence.tenant;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité de persistance R2DBC pour le Tenant.
 * Adapteur de la couche Infrastructure — traduit entre le domaine et la DB.
 * Le domaine Tenant ne connaît pas cette classe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("bcaas_tenants")
public class TenantEntity {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("slug")
    private String slug;

    @Column("status")
    private String status;

    @Column("plan")
    private String plan;

    @Column("default_locale")
    private String defaultLocale;

    @Column("default_currency")
    private String defaultCurrency;

    @Column("timezone")
    private String timezone;

    @Column("notifications_enabled")
    private boolean notificationsEnabled;

    @Column("audit_enabled")
    private boolean auditEnabled;

    @Column("session_timeout_minutes")
    private int sessionTimeoutMinutes;

    @Column("created_at")
    private Instant createdAt;

    @Column("created_by")
    private UUID createdBy;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("updated_by")
    private UUID updatedBy;

    // ================================================================
    // Mapping Domaine → Entité
    // ================================================================

    public static TenantEntity fromDomain(Tenant tenant) {
        TenantSettings settings = tenant.getSettings();
        AuditInfo audit = tenant.getAuditInfo();

        return TenantEntity.builder()
                .id(tenant.getId().value())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus().name())
                .plan(tenant.getPlan().name())
                .defaultLocale(settings.defaultLocale())
                .defaultCurrency(settings.defaultCurrency())
                .timezone(settings.timezone())
                .notificationsEnabled(settings.notificationsEnabled())
                .auditEnabled(settings.auditEnabled())
                .sessionTimeoutMinutes(settings.sessionTimeoutMinutes())
                .createdAt(audit.createdAt())
                .createdBy(audit.createdBy().value())
                .updatedAt(audit.updatedAt())
                .updatedBy(audit.updatedBy().value())
                .build();
    }

    // ================================================================
    // Mapping Entité → Domaine
    // ================================================================

    public Tenant toDomain() {
        TenantSettings settings = new TenantSettings(
                defaultLocale, defaultCurrency, timezone,
                notificationsEnabled, auditEnabled, sessionTimeoutMinutes
        );
        AuditInfo auditInfo = new AuditInfo(
                createdAt, ActorId.of(createdBy),
                updatedAt, ActorId.of(updatedBy)
        );
        return Tenant.reconstitute(
                TenantId.of(id), name, slug,
                TenantStatus.valueOf(status),
                TenantPlan.valueOf(plan),
                settings, auditInfo
        );
    }
}
