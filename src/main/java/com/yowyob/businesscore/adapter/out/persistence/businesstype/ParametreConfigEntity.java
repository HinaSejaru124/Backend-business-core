package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Projection R2DBC de la table parametre_config (tenant-owned, soumise à RLS). */
@Table("parametre_config")
public class ParametreConfigEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("version_type_id")
    private UUID versionTypeId;

    @Column("entreprise_id")
    private UUID entrepriseId;

    private String cle;
    private String valeur;
    private boolean verrouille;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public ParametreConfigEntity() {}

    public static ParametreConfigEntity nouveau(UUID id, UUID tenantId,
                                                UUID versionTypeId, UUID entrepriseId,
                                                String cle, String valeur,
                                                boolean verrouille) {
        ParametreConfigEntity e = new ParametreConfigEntity();
        e.id           = id;
        e.tenantId     = tenantId;
        e.versionTypeId = versionTypeId;
        e.entrepriseId = entrepriseId;
        e.cle          = cle;
        e.valeur       = valeur;
        e.verrouille   = verrouille;
        e.createdAt    = Instant.now();
        e.nouveau      = true;
        return e;
    }

    @Override public UUID getId()    { return id; }
    @Override public boolean isNew() { return nouveau; }

    public UUID getTenantId()       { return tenantId; }
    public UUID getVersionTypeId()  { return versionTypeId; }
    public UUID getEntrepriseId()   { return entrepriseId; }
    public String getCle()          { return cle; }
    public String getValeur()       { return valeur; }
    public boolean isVerrouille()   { return verrouille; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setId(UUID id)                      { this.id = id; }
    public void setTenantId(UUID tenantId)          { this.tenantId = tenantId; }
    public void setVersionTypeId(UUID v)            { this.versionTypeId = v; }
    public void setEntrepriseId(UUID e)             { this.entrepriseId = e; }
    public void setCle(String cle)                  { this.cle = cle; }
    public void setValeur(String valeur)            { this.valeur = valeur; }
    public void setVerrouille(boolean v)            { this.verrouille = v; }
    public void setCreatedAt(Instant createdAt)     { this.createdAt = createdAt; }
}
