package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code entreprise_contrat} (tenant-owned, soumise à RLS).
 * Clé primaire = {@code entreprise_id} (relation 1:1 avec {@code entreprise}).
 */
@Table("entreprise_contrat")
public class EntrepriseContratEntity implements Persistable<UUID> {

    @Id
    @Column("entreprise_id")
    private UUID entrepriseId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("cle_publique")
    private String clePublique;

    @Column("callback_url")
    private String callbackUrl;

    @Column("success_url")
    private String successUrl;

    @Column("error_url")
    private String errorUrl;

    @Column("cancel_url")
    private String cancelUrl;

    @Column("mis_a_jour_le")
    private Instant misAJourLe;

    @Transient
    private boolean nouveau;

    public EntrepriseContratEntity() {
    }

    public static EntrepriseContratEntity nouveau(UUID entrepriseId, UUID tenantId, String clePublique,
                                                  String callbackUrl, String successUrl, String errorUrl,
                                                  String cancelUrl, Instant misAJourLe) {
        EntrepriseContratEntity e = new EntrepriseContratEntity();
        e.entrepriseId = entrepriseId;
        e.tenantId = tenantId;
        e.clePublique = clePublique;
        e.callbackUrl = callbackUrl;
        e.successUrl = successUrl;
        e.errorUrl = errorUrl;
        e.cancelUrl = cancelUrl;
        e.misAJourLe = misAJourLe;
        e.nouveau = true;
        return e;
    }

    @Override
    public UUID getId() {
        return entrepriseId;
    }

    @Override
    public boolean isNew() {
        return nouveau;
    }

    public UUID getEntrepriseId() {
        return entrepriseId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getClePublique() {
        return clePublique;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public String getErrorUrl() {
        return errorUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public Instant getMisAJourLe() {
        return misAJourLe;
    }

    public void setEntrepriseId(UUID entrepriseId) {
        this.entrepriseId = entrepriseId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void setClePublique(String clePublique) {
        this.clePublique = clePublique;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public void setErrorUrl(String errorUrl) {
        this.errorUrl = errorUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public void setMisAJourLe(Instant misAJourLe) {
        this.misAJourLe = misAJourLe;
    }
}
