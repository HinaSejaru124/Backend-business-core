package com.yowyob.businesscore.adapter.out.persistence.operation;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code definition_operation} (tenant-owned, soumise à RLS).
 * Identifiant généré côté application + {@link Persistable} pour forcer un INSERT.
 */
@Table("definition_operation")
public class DefinitionOperationEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("version_type_id")
    private UUID versionTypeId;

    private String nom;

    @Column("role_declencheur")
    private String roleDeclencheur;

    @Column("declencheur_regles")
    private String declencheurRegles;

    private boolean differe;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public DefinitionOperationEntity() {
    }

    public static DefinitionOperationEntity nouveau(UUID id, UUID tenantId, UUID versionTypeId, String nom,
                                                    String roleDeclencheur, String declencheurRegles,
                                                    boolean differe) {
        DefinitionOperationEntity e = new DefinitionOperationEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.versionTypeId = versionTypeId;
        e.nom = nom;
        e.roleDeclencheur = roleDeclencheur;
        e.declencheurRegles = declencheurRegles;
        e.differe = differe;
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

    public UUID getVersionTypeId() {
        return versionTypeId;
    }

    public String getNom() {
        return nom;
    }

    public String getRoleDeclencheur() {
        return roleDeclencheur;
    }

    public String getDeclencheurRegles() {
        return declencheurRegles;
    }

    public boolean isDiffere() {
        return differe;
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

    public void setVersionTypeId(UUID versionTypeId) {
        this.versionTypeId = versionTypeId;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setRoleDeclencheur(String roleDeclencheur) {
        this.roleDeclencheur = roleDeclencheur;
    }

    public void setDeclencheurRegles(String declencheurRegles) {
        this.declencheurRegles = declencheurRegles;
    }

    public void setDiffere(boolean differe) {
        this.differe = differe;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
