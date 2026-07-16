package com.yowyob.businesscore.adapter.out.persistence.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code etape_operation} (tenant-owned, soumise à RLS).
 */
@Table("etape_operation")
public class EtapeOperationEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("operation_id")
    private UUID operationId;

    private int ordre;

    @Column("type_etape")
    private String typeEtape;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public EtapeOperationEntity() {
    }

    public static EtapeOperationEntity nouveau(UUID id, UUID tenantId, UUID operationId, int ordre,
                                               String typeEtape) {
        EtapeOperationEntity e = new EtapeOperationEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.operationId = operationId;
        e.ordre = ordre;
        e.typeEtape = typeEtape;
        e.nouveau = true;
        return e;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nouveau;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public int getOrdre() {
        return ordre;
    }

    public String getTypeEtape() {
        return typeEtape;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public void setTypeEtape(String typeEtape) {
        this.typeEtape = typeEtape;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
