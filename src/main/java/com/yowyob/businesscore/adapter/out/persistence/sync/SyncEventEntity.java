package com.yowyob.businesscore.adapter.out.persistence.sync;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code sync_event} (tenant-owned, soumise à RLS). Append-only : jamais
 * modifiée après insertion. {@code id} est le curseur global (bigserial) utilisé par le terminal pour
 * son paramètre {@code since}.
 */
@Table("sync_event")
public class SyncEventEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("entreprise_id")
    private UUID entrepriseId;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private UUID entityId;

    private String operation;

    private String payload;

    @Column("created_at")
    private Instant createdAt;

    public SyncEventEntity() {
    }

    public static SyncEventEntity nouveau(UUID tenantId, UUID entrepriseId, String entityType,
                                          UUID entityId, String operation, String payload) {
        SyncEventEntity e = new SyncEventEntity();
        e.tenantId = tenantId;
        e.entrepriseId = entrepriseId;
        e.entityType = entityType;
        e.entityId = entityId;
        e.operation = operation;
        e.payload = payload;
        return e;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getEntrepriseId() {
        return entrepriseId;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getOperation() {
        return operation;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
