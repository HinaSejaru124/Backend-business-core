package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code entreprise} (tenant-owned, soumise à RLS).
 * Version minimale (brique Entreprise) destinée à être complétée par Dev 3.
 */
@Table("entreprise")
public class EntrepriseEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("type_metier_id")
    private UUID typeMetierId;

    @Column("version_type_id")
    private UUID versionTypeId;

    @Column("numero_version")
    private int numeroVersion;

    @Column("organization_id")
    private UUID organizationId;

    @Column("business_actor_id")
    private UUID businessActorId;

    @Column("agency_id")
    private UUID agencyId;

    private String nom;

    @Column("cycle_vie")
    private String cycleVie;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public EntrepriseEntity() {
    }

    public static EntrepriseEntity nouveau(UUID id, UUID tenantId, UUID typeMetierId, UUID versionTypeId,
                                           int numeroVersion, UUID organizationId, UUID businessActorId,
                                           UUID agencyId, String nom, String cycleVie) {
        EntrepriseEntity e = new EntrepriseEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.typeMetierId = typeMetierId;
        e.versionTypeId = versionTypeId;
        e.numeroVersion = numeroVersion;
        e.organizationId = organizationId;
        e.businessActorId = businessActorId;
        e.agencyId = agencyId;
        e.nom = nom;
        e.cycleVie = cycleVie;
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

    public UUID getTypeMetierId() {
        return typeMetierId;
    }

    public UUID getVersionTypeId() {
        return versionTypeId;
    }

    public int getNumeroVersion() {
        return numeroVersion;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getBusinessActorId() {
        return businessActorId;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public String getNom() {
        return nom;
    }

    public String getCycleVie() {
        return cycleVie;
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

    public void setTypeMetierId(UUID typeMetierId) {
        this.typeMetierId = typeMetierId;
    }

    public void setVersionTypeId(UUID versionTypeId) {
        this.versionTypeId = versionTypeId;
    }

    public void setNumeroVersion(int numeroVersion) {
        this.numeroVersion = numeroVersion;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void setBusinessActorId(UUID businessActorId) {
        this.businessActorId = businessActorId;
    }

    public void setAgencyId(UUID agencyId) {
        this.agencyId = agencyId;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setCycleVie(String cycleVie) {
        this.cycleVie = cycleVie;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
