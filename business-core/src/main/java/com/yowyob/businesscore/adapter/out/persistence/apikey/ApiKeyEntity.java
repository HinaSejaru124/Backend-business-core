package com.yowyob.businesscore.adapter.out.persistence.apikey;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Clé API d'une entreprise. Une entreprise n'a jamais plus d'une clé {@code ACTIVE} à la fois (imposé
 * à la création). Le secret n'est stocké que haché ({@code key_hash}) — pas de {@code prefix} : le
 * développeur s'identifie par son {@code developerId} stable ({@code X-BC-Client-Id}, cf.
 * {@code GET /v1/auth/me}), l'entreprise est résolue en confrontant le secret aux hachés des clés
 * actives de ce développeur. La révocation est immédiate ({@code status=REVOKED}).
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

    @Column("entreprise_id")
    private UUID entrepriseId;

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

    public static ApiKeyEntity nouveau(UUID id, UUID developerId, UUID entrepriseId, String keyHash, String name) {
        ApiKeyEntity e = new ApiKeyEntity();
        e.id = id;
        e.developerId = developerId;
        e.entrepriseId = entrepriseId;
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

    public UUID getEntrepriseId() {
        return entrepriseId;
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

    public void setEntrepriseId(UUID entrepriseId) {
        this.entrepriseId = entrepriseId;
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
