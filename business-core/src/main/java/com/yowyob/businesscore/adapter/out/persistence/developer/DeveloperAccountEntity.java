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
 * <p>Le tenant métier est le {@code kernel_tenant_id} (le {@code tid} du kernel), renseigné au sign-up
 * puis confirmé au login. Toutes les données métier sont isolées sur ce tenant, que l'appel soit
 * authentifié par une clé API (backend du dev) ou par un JWT utilisateur : les deux résolvent le même
 * tenant. Cette table n'a pas de RLS (elle sert précisément à établir le tenant avant qu'il soit connu).
 *
 * <p>Les clés API vivent désormais dans la table {@code api_key} (plusieurs clés par développeur).
 *
 * <p>Implémente {@link Persistable} avec un identifiant généré côté application : on contrôle l'UUID
 * et on signale explicitement l'état "nouveau" pour que Spring Data émette un INSERT (et non un UPDATE).
 */
@Table("developer_account")
public class DeveloperAccountEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private String email;

    @Column("kernel_tenant_id")
    private UUID kernelTenantId;

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

    public static DeveloperAccountEntity nouveau(UUID id, String email, UUID kernelTenantId,
                                                 String kernelClientId, String kernelSecretEncrypted,
                                                 String plan) {
        DeveloperAccountEntity e = new DeveloperAccountEntity();
        e.id = id;
        e.email = email;
        e.kernelTenantId = kernelTenantId;
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

    public String getEmail() {
        return email;
    }

    public UUID getKernelTenantId() {
        return kernelTenantId;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public void setKernelTenantId(UUID kernelTenantId) {
        this.kernelTenantId = kernelTenantId;
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
