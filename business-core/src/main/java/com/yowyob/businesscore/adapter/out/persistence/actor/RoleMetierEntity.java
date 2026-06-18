package com.yowyob.businesscore.adapter.out.persistence.actor;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("role_metier")
public class RoleMetierEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("tenant_id")
    private UUID tenantId;
    @Column("version_type_id")
    private UUID versionTypeId;
    private String code;
    private String categorie;
    @Column("created_at")
    private Instant createdAt;
    @Transient
    private boolean nouveau;

    public RoleMetierEntity() {
    }

    public static RoleMetierEntity nouveau(UUID id, UUID tenantId, UUID versionTypeId,
                                           String code, String categorie) {
        RoleMetierEntity e = new RoleMetierEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.versionTypeId = versionTypeId;
        e.code = code;
        e.categorie = categorie;
        e.createdAt = Instant.now();
        e.nouveau = true;
        return e;
    }

    @Override
    public UUID getId() { return id; }
    @Override
    public boolean isNew() { return nouveau; }

    public UUID getTenantId() { return tenantId; }
    public UUID getVersionTypeId() { return versionTypeId; }
    public String getCode() { return code; }
    public String getCategorie() { return categorie; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setVersionTypeId(UUID versionTypeId) { this.versionTypeId = versionTypeId; }
    public void setCode(String code) { this.code = code; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}