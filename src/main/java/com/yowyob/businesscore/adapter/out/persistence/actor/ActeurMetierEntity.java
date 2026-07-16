package com.yowyob.businesscore.adapter.out.persistence.actor;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("acteur_metier")
public class ActeurMetierEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("tenant_id")
    private UUID tenantId;
    @Column("entreprise_id")
    private UUID entrepriseId;
    @Column("role_metier_id")
    private UUID roleMetierId;
    @Column("acteur_kernel_id")
    private UUID acteurKernelId;
    @Column("valide_depuis")
    private Instant valideDepuis;
    @Column("valide_jusqua")
    private Instant valideJusqua;
    @Transient
    private boolean nouveau;

    public ActeurMetierEntity() {
    }

    public static ActeurMetierEntity nouveau(UUID id, UUID tenantId, UUID entrepriseId, UUID roleMetierId,
                                             UUID acteurKernelId, Instant valideDepuis, Instant valideJusqua) {
        ActeurMetierEntity e = new ActeurMetierEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.entrepriseId = entrepriseId;
        e.roleMetierId = roleMetierId;
        e.acteurKernelId = acteurKernelId;
        e.valideDepuis = valideDepuis;
        e.valideJusqua = valideJusqua;
        e.nouveau = true;
        return e;
    }

    /** Reconstruit pour un UPDATE (ex. détachement) : isNew()=false. */
    public ActeurMetierEntity enModification() {
        this.nouveau = false;
        return this;
    }

    @Override
    public UUID getId() { return id; }
    @Override
    public boolean isNew() { return nouveau; }

    public UUID getTenantId() { return tenantId; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public UUID getRoleMetierId() { return roleMetierId; }
    public UUID getActeurKernelId() { return acteurKernelId; }
    public Instant getValideDepuis() { return valideDepuis; }
    public Instant getValideJusqua() { return valideJusqua; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public void setRoleMetierId(UUID roleMetierId) { this.roleMetierId = roleMetierId; }
    public void setActeurKernelId(UUID acteurKernelId) { this.acteurKernelId = acteurKernelId; }
    public void setValideDepuis(Instant valideDepuis) { this.valideDepuis = valideDepuis; }
    public void setValideJusqua(Instant valideJusqua) { this.valideJusqua = valideJusqua; }
}