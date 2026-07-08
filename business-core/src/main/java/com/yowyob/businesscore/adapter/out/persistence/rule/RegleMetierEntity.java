package com.yowyob.businesscore.adapter.out.persistence.rule;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code regle_metier}.
 */
@Table("regle_metier")
public class RegleMetierEntity implements Persistable<UUID> {

    @Id private UUID id;
    @Column("tenant_id")     private UUID tenantId;
    @Column("version_type_id") private UUID versionTypeId;
    @Column("entreprise_id") private UUID entrepriseId;
    @Column("declencheur")   private String declencheur;
    @Column("condition")     private String condition;
    @Column("effet")         private String effet;
    @Column("roles_autorises_a_deroger") private List<String> rolesAutorisesADeroger;
    @Column("created_at")    private OffsetDateTime createdAt;
    @Column("updated_at")    private OffsetDateTime updatedAt;
    @Transient private boolean nouveau = true;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return nouveau; }

    public void enModification() { this.nouveau = false; }

    public UUID getTenantId()                         { return tenantId; }
    public void setTenantId(UUID v)                   { this.tenantId = v; }
    public void setId(UUID v)                         { this.id = v; }
    public UUID getVersionTypeId()                    { return versionTypeId; }
    public void setVersionTypeId(UUID v)              { this.versionTypeId = v; }
    public UUID getEntrepriseId()                     { return entrepriseId; }
    public void setEntrepriseId(UUID v)               { this.entrepriseId = v; }
    public String getDeclencheur()                    { return declencheur; }
    public void setDeclencheur(String v)              { this.declencheur = v; }
    public String getCondition()                      { return condition; }
    public void setCondition(String v)                { this.condition = v; }
    public String getEffet()                          { return effet; }
    public void setEffet(String v)                    { this.effet = v; }
    public List<String> getRolesAutorisesADeroger()   { return rolesAutorisesADeroger; }
    public void setRolesAutorisesADeroger(List<String> v) { this.rolesAutorisesADeroger = v; }
    public OffsetDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(OffsetDateTime v)        { this.createdAt = v; }
    public OffsetDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime v)        { this.updatedAt = v; }
}
