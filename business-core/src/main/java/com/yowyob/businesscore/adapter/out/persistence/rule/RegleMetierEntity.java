// adapter/out/persistence/rule/RegleMetierEntity.java
package com.yowyob.businesscore.adapter.out.persistence.rule;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code regle_metier}.
 *
 * <p>{@code declencheur} et {@code effet} sont stockés sous forme de chaîne (le nom de l'enum) ;
 * {@code condition} est la condition N1 brute (« TYPE:param=val ») ; {@code rolesAutorisesADeroger}
 * est mappé sur une colonne PostgreSQL {@code text[]}. La table porte une colonne {@code tenant_id}
 * soumise à la Row-Level Security (isolation par tenant).
 */
@Table("regle_metier")
public class RegleMetierEntity {

    @Id private UUID id;
    @Column("tenant_id")     private UUID tenantId;
    @Column("version_type_id") private UUID versionTypeId;
    @Column("entreprise_id") private UUID entrepriseId;
    @Column("declencheur")   private String declencheur;
    @Column("condition")     private String condition;   // format "TYPE:param=val"
    @Column("effet")         private String effet;
    @Column("roles_autorises_a_deroger") private List<String> rolesAutorisesADeroger;
    @Column("created_at")    private OffsetDateTime createdAt;
    @Column("updated_at")    private OffsetDateTime updatedAt;

    public UUID getId()                               { return id; }
    public void setId(UUID v)                         { this.id = v; }
    public UUID getTenantId()                         { return tenantId; }
    public void setTenantId(UUID v)                   { this.tenantId = v; }
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