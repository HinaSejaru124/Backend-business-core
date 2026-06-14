package com.yowyob.businesscore.adapter.out.persistence.developer;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte développeur — registre d'authentification et porteur du tenant.
 *
 * <p>DÉFINIT le tenant : {@code id} sert de {@code tenant_id} pour toutes les données métier. Cette
 * table n'a donc pas de RLS (elle serait sinon impossible à interroger avant que le tenant soit connu).
 *
 * <p>Implémente {@link Persistable} avec un identifiant généré côté application : on contrôle l'UUID
 * et on signale explicitement l'état "nouveau" pour que Spring Data émette un INSERT (et non un UPDATE).
 */
@Table("developer_account")
public class DeveloperAccountEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("bc_client_id")
    private String bcClientId;

    @Column("bc_api_key_hash")
    private String bcApiKeyHash;

    @Column("kernel_client_id")
    private String kernelClientId;

    @Column("kernel_secret_encrypted")
    private String kernelSecretEncrypted;

    private String plan;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public DeveloperAccountEntity() {
    }

    public static DeveloperAccountEntity nouveau(UUID id, String bcClientId, String bcApiKeyHash,
                                                 String kernelClientId, String kernelSecretEncrypted,
                                                 String plan) {
        DeveloperAccountEntity e = new DeveloperAccountEntity();
        e.id = id;
        e.bcClientId = bcClientId;
        e.bcApiKeyHash = bcApiKeyHash;
        e.kernelClientId = kernelClientId;
        e.kernelSecretEncrypted = kernelSecretEncrypted;
        e.plan = plan;
        e.status = "ACTIVE";
        e.createdAt = Instant.now();
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

    public String getBcClientId() {
        return bcClientId;
    }

    public String getBcApiKeyHash() {
        return bcApiKeyHash;
    }

    public String getKernelClientId() {
        return kernelClientId;
    }

    public String getKernelSecretEncrypted() {
        return kernelSecretEncrypted;
    }

    public String getPlan() {
        return plan;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setBcClientId(String bcClientId) {
        this.bcClientId = bcClientId;
    }

    public void setBcApiKeyHash(String bcApiKeyHash) {
        this.bcApiKeyHash = bcApiKeyHash;
    }

    public void setKernelClientId(String kernelClientId) {
        this.kernelClientId = kernelClientId;
    }

    public void setKernelSecretEncrypted(String kernelSecretEncrypted) {
        this.kernelSecretEncrypted = kernelSecretEncrypted;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
