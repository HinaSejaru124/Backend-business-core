package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code type_metier} (tenant-owned, soumise à RLS).
 * Identifiant généré côté application + {@link Persistable} pour forcer un INSERT.
 */
@Table("type_metier")
public class TypeMetierEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("business_domain_id")
    private UUID businessDomainId;

    private String code;
    private String nom;
    private String statut;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public TypeMetierEntity() {
    }

    public static TypeMetierEntity nouveau(UUID id, UUID tenantId, UUID businessDomainId,
                                           String code, String nom, String statut) {
        TypeMetierEntity e = new TypeMetierEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.businessDomainId = businessDomainId;
        e.code = code;
        e.nom = nom;
        e.statut = statut;
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

    public UUID getBusinessDomainId() {
        return businessDomainId;
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    public String getStatut() {
        return statut;
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

    public void setBusinessDomainId(UUID businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
