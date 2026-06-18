package com.yowyob.businesscore.adapter.out.persistence.offer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("capacite")
public class CapaciteEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("tenant_id")
    private UUID tenantId;
    @Column("definition_offre_id")
    private UUID definitionOffreId;
    private String type;
    private boolean active;
    @Column("created_at")
    private Instant createdAt;
    @Transient
    private boolean nouveau;

    public CapaciteEntity() {
    }

    public static CapaciteEntity nouveau(UUID id, UUID tenantId, UUID definitionOffreId,
                                         String type, boolean active) {
        CapaciteEntity e = new CapaciteEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.definitionOffreId = definitionOffreId;
        e.type = type;
        e.active = active;
        e.createdAt = Instant.now();
        e.nouveau = true;
        return e;
    }

    @Override
    public UUID getId() { return id; }
    @Override
    public boolean isNew() { return nouveau; }

    public UUID getTenantId() { return tenantId; }
    public UUID getDefinitionOffreId() { return definitionOffreId; }
    public String getType() { return type; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setDefinitionOffreId(UUID definitionOffreId) { this.definitionOffreId = definitionOffreId; }
    public void setType(String type) { this.type = type; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}