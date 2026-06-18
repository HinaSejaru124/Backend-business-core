package com.yowyob.businesscore.adapter.out.persistence.trace;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection R2DBC de la table {@code trace_operation} (tenant-owned, soumise à RLS).
 * Mutable au fil de l'opération (EN_COURS → COMPLETEE / COMPENSEE) : un UPDATE est appliqué quand la
 * ligne existe déjà, sinon un INSERT (cf. {@code isNew()}).
 */
@Table("trace_operation")
public class TraceOperationEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("entreprise_id")
    private UUID entrepriseId;

    @Column("operation_id")
    private UUID operationId;

    @Column("operation_nom")
    private String operationNom;

    @Column("cle_idempotence")
    private String cleIdempotence;

    @Column("transaction_kernel_id")
    private UUID transactionKernelId;

    private String statut;

    @Column("resultat_regles")
    private String resultatRegles;

    @Column("cree_le")
    private Instant creeLe;

    @Column("resolu_le")
    private Instant resoluLe;

    @Transient
    private boolean nouveau;

    public TraceOperationEntity() {
    }

    public static TraceOperationEntity nouveau(UUID id, UUID tenantId, UUID entrepriseId, UUID operationId,
                                               String operationNom, String cleIdempotence,
                                               UUID transactionKernelId, String statut, String resultatRegles,
                                               Instant creeLe, Instant resoluLe) {
        TraceOperationEntity e = new TraceOperationEntity();
        e.id = id;
        e.tenantId = tenantId;
        e.entrepriseId = entrepriseId;
        e.operationId = operationId;
        e.operationNom = operationNom;
        e.cleIdempotence = cleIdempotence;
        e.transactionKernelId = transactionKernelId;
        e.statut = statut;
        e.resultatRegles = resultatRegles;
        e.creeLe = creeLe;
        e.resoluLe = resoluLe;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getEntrepriseId() {
        return entrepriseId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public String getOperationNom() {
        return operationNom;
    }

    public String getCleIdempotence() {
        return cleIdempotence;
    }

    public UUID getTransactionKernelId() {
        return transactionKernelId;
    }

    public String getStatut() {
        return statut;
    }

    public String getResultatRegles() {
        return resultatRegles;
    }

    public Instant getCreeLe() {
        return creeLe;
    }

    public Instant getResoluLe() {
        return resoluLe;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void setEntrepriseId(UUID entrepriseId) {
        this.entrepriseId = entrepriseId;
    }

    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }

    public void setOperationNom(String operationNom) {
        this.operationNom = operationNom;
    }

    public void setCleIdempotence(String cleIdempotence) {
        this.cleIdempotence = cleIdempotence;
    }

    public void setTransactionKernelId(UUID transactionKernelId) {
        this.transactionKernelId = transactionKernelId;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setResultatRegles(String resultatRegles) {
        this.resultatRegles = resultatRegles;
    }

    public void setCreeLe(Instant creeLe) {
        this.creeLe = creeLe;
    }

    public void setResoluLe(Instant resoluLe) {
        this.resoluLe = resoluLe;
    }
}
