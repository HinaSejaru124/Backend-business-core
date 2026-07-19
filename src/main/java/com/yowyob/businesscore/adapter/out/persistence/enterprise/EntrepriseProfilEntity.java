package com.yowyob.businesscore.adapter.out.persistence.enterprise;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code entreprise_profil} (tenant-owned, soumise à RLS).
 * Clé primaire = {@code entreprise_id} (relation 1:1 avec {@code entreprise}).
 */
@Table("entreprise_profil")
public class EntrepriseProfilEntity implements Persistable<UUID> {

    @Id
    @Column("entreprise_id")
    private UUID entrepriseId;

    @Column("tenant_id")
    private UUID tenantId;

    private String description;

    @Column("logo_url")
    private String logoUrl;

    private String couleur;

    @Column("support_email")
    private String supportEmail;

    @Column("site_web_url")
    private String siteWebUrl;

    private String environnement;

    @Column("mis_a_jour_le")
    private Instant misAJourLe;

    @Transient
    private boolean nouveau;

    public EntrepriseProfilEntity() {
    }

    public static EntrepriseProfilEntity nouveau(UUID entrepriseId, UUID tenantId, String description,
                                                 String logoUrl, String couleur, String supportEmail,
                                                 String siteWebUrl, String environnement, Instant misAJourLe) {
        EntrepriseProfilEntity e = new EntrepriseProfilEntity();
        e.entrepriseId = entrepriseId;
        e.tenantId = tenantId;
        e.description = description;
        e.logoUrl = logoUrl;
        e.couleur = couleur;
        e.supportEmail = supportEmail;
        e.siteWebUrl = siteWebUrl;
        e.environnement = environnement;
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

    public String getDescription() {
        return description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getCouleur() {
        return couleur;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public String getSiteWebUrl() {
        return siteWebUrl;
    }

    public String getEnvironnement() {
        return environnement;
    }

    public Instant getMisAJourLe() {
        return misAJourLe;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public void setSiteWebUrl(String siteWebUrl) {
        this.siteWebUrl = siteWebUrl;
    }

    public void setEnvironnement(String environnement) {
        this.environnement = environnement;
    }

    public void setMisAJourLe(Instant misAJourLe) {
        this.misAJourLe = misAJourLe;
    }
}
