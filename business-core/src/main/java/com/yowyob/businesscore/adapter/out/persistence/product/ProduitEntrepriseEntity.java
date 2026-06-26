package com.yowyob.businesscore.adapter.out.persistence.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapping persisté offre ↔ produit kernel, par entreprise (table {@code produit_entreprise}). */
@Table("produit_entreprise")
public class ProduitEntrepriseEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column("tenant_id")
    private UUID tenantId;
    @Column("entreprise_id")
    private UUID entrepriseId;
    @Column("offre_id")
    private UUID offreId;
    @Column("product_id")
    private UUID productId;
    @Column("created_at")
    private Instant createdAt;
    @Transient
    private boolean nouveau;

    public ProduitEntrepriseEntity() {
    }

    public static ProduitEntrepriseEntity nouveau(UUID id, UUID tenantId, UUID entrepriseId,
                                                  UUID offreId, UUID productId) {
        ProduitEntrepriseEntity e = new ProduitEntrepriseEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.entrepriseId = entrepriseId;
        e.offreId = offreId;
        e.productId = productId;
        e.createdAt = Instant.now();
        e.nouveau = true;
        return e;
    }

    @Override
    public UUID getId() { return id; }
    @Override
    public boolean isNew() { return nouveau; }

    public UUID getTenantId() { return tenantId; }
    public UUID getEntrepriseId() { return entrepriseId; }
    public UUID getOffreId() { return offreId; }
    public UUID getProductId() { return productId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public void setEntrepriseId(UUID entrepriseId) { this.entrepriseId = entrepriseId; }
    public void setOffreId(UUID offreId) { this.offreId = offreId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
