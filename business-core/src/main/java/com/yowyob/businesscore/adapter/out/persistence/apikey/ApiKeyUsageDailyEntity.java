package com.yowyob.businesscore.adapter.out.persistence.apikey;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Agrégat quotidien d'usage d'une clé API (alimenté par le flush des compteurs Redis). Sert le
 * graphique 30 jours et les totaux du dashboard. En lecture seule côté application (upsert via requête).
 */
@Table("api_key_usage_daily")
public class ApiKeyUsageDailyEntity {

    @Id
    private UUID id;

    @Column("api_key_id")
    private UUID apiKeyId;

    private LocalDate jour;

    private long total;

    private long errors;

    @Column("p95_ms")
    private long p95Ms;

    public UUID getId() {
        return id;
    }

    public UUID getApiKeyId() {
        return apiKeyId;
    }

    public LocalDate getJour() {
        return jour;
    }

    public long getTotal() {
        return total;
    }

    public long getErrors() {
        return errors;
    }

    public long getP95Ms() {
        return p95Ms;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setApiKeyId(UUID apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public void setJour(LocalDate jour) {
        this.jour = jour;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setErrors(long errors) {
        this.errors = errors;
    }

    public void setP95Ms(long p95Ms) {
        this.p95Ms = p95Ms;
    }
}
