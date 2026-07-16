package com.yowyob.businesscore.adapter.out.persistence.businesstype;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Projection R2DBC de la table {@code version_type} (tenant-owned, soumise à RLS). */
@Table("version_type")
public class VersionTypeEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("type_metier_id")
    private UUID typeMetierId;

    private int numero;
    private boolean immuable;

    @Column("publiee_le")
    private Instant publieeLe;

    @Transient
    private boolean nouveau;

    public VersionTypeEntity() {
    }

    public static VersionTypeEntity nouveau(UUID id, UUID tenantId, UUID typeMetierId,
                                            int numero, boolean immuable, Instant publieeLe) {
        VersionTypeEntity e = new VersionTypeEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.typeMetierId = typeMetierId;
        e.numero = numero;
        e.immuable = immuable;
        e.publieeLe = publieeLe;
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

    public int getNumero() {
        return numero;
    }

    public boolean isImmuable() {
        return immuable;
    }

    public Instant getPublieeLe() {
        return publieeLe;
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

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public void setImmuable(boolean immuable) {
        this.immuable = immuable;
    }

    public void setPublieeLe(Instant publieeLe) {
        this.publieeLe = publieeLe;
    }
}
