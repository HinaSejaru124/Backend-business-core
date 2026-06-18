package com.yowyob.businesscore.adapter.out.persistence.offer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("definition_offre")
public class DefinitionOffreEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("tenant_id")
    private UUID tenantId;
    @Column("version_type_id")
    private UUID versionTypeId;
    private String nom;
    @Column("forme_prix")
    private String formePrix;
    private BigDecimal prix;
    @Column("created_at")
    private Instant createdAt;
    @Transient
    private boolean nouveau;

    public DefinitionOffreEntity() {
    }

    public static DefinitionOffreEntity nouveau(UUID id, UUID tenantId, UUID versionTypeId,
                                                String nom, String formePrix, BigDecimal prix) {
        DefinitionOffreEntity e = new DefinitionOffreEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.versionTypeId = versionTypeId;
        e.nom = nom;
        e.formePrix = formePrix;
        e.prix = prix;
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
    public String getNom() { return nom; }
    public String getFormePrix() { return formePrix; }
    public BigDecimal getPrix() { return prix; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setVersionTypeId(UUID versionTypeId) { this.versionTypeId = versionTypeId; }
    public void setNom(String nom) { this.nom = nom; }
    public void setFormePrix(String formePrix) { this.formePrix = formePrix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}