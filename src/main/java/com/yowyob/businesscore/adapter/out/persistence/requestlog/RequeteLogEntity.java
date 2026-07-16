package com.yowyob.businesscore.adapter.out.persistence.requestlog;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code requete_log} (tenant-owned, soumise à RLS).
 * Insert-only — jamais de mise à jour (chaque ligne est une requête déjà terminée).
 */
@Table("requete_log")
public class RequeteLogEntity {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    private String categorie;

    private String methode;

    private String endpoint;

    @Column("statut_http")
    private Integer statutHttp;

    @Column("duree_ms")
    private Long dureeMs;

    /** true = compte réellement dans le quota du plan (clé API / runtime) ; false = design-time (JWT). */
    private Boolean facturable;

    @Column("cree_le")
    private Instant creeLe;

    public RequeteLogEntity() {
    }

    public static RequeteLogEntity nouvelle(UUID tenantId, String categorie, String methode, String endpoint,
                                            int statutHttp, long dureeMs, boolean facturable, Instant creeLe) {
        RequeteLogEntity e = new RequeteLogEntity();
        e.tenantId = tenantId;
        e.categorie = categorie;
        e.methode = methode;
        e.endpoint = endpoint;
        e.statutHttp = statutHttp;
        e.dureeMs = dureeMs;
        e.facturable = facturable;
        e.creeLe = creeLe;
        return e;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCategorie() {
        return categorie;
    }

    public String getMethode() {
        return methode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Integer getStatutHttp() {
        return statutHttp;
    }

    public Long getDureeMs() {
        return dureeMs;
    }

    public Boolean getFacturable() {
        return facturable;
    }

    public Instant getCreeLe() {
        return creeLe;
    }
}
