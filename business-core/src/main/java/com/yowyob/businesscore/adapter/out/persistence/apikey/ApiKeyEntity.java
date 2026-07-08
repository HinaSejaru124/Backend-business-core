package com.yowyob.businesscore.adapter.out.persistence.apikey;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Clé API d'un développeur. Un développeur peut en détenir plusieurs (« Prod », « Dev »…). Le secret
 * n'est stocké que haché ({@code key_hash}) ; seul le {@code prefix} (l'identifiant public, porté par
 * l'en-tête {@code X-BC-Client-Id}) reste lisible. La révocation est immédiate ({@code status=REVOKED}).
 *
 * <p>Table sans RLS : elle sert à résoudre le tenant avant qu'il soit connu.
 */
@Table("api_key")
public class ApiKeyEntity implements Persistable<UUID> {

    public static final String STATUT_ACTIVE = "ACTIVE";
    public static final String STATUT_REVOKED = "REVOKED";

    @Id
    private UUID id;

    @Column("developer_id")
    private UUID developerId;

    private String prefix;

    @Column("key_hash")
    private String keyHash;

    private String name;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("last_used_at")
    private Instant lastUsedAt;

    @Transient
    private boolean nouveau;

    public ApiKeyEntity() {
    }

    public static ApiKeyEntity nouveau(UUID id, UUID developerId, String prefix, String keyHash,
                                       String name) {
        ApiKeyEntity e = new ApiKeyEntity();
        e.id = id;
        e.developerId = developerId;
        e.prefix = prefix;
        e.keyHash = keyHash;
        e.name = (name == null || name.isBlank()) ? "Default" : name;
        e.status = STATUT_ACTIVE;
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

    public UUID getDeveloperId() {
        return developerId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public boolean estActive() {
        return STATUT_ACTIVE.equals(status);
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setDeveloperId(UUID developerId) {
        this.developerId = developerId;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
