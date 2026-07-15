package com.pharmacore.pharmaciebackend.vente;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vente")
public class Vente {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "ordonnance_id")
    private UUID ordonnanceId;

    @Column(name = "montant_total", nullable = false)
    private BigDecimal montantTotal;

    @Column(nullable = false)
    private String devise;

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement;

    @Column(name = "statut_bcaas", nullable = false)
    private String statutBcaas;

    @Column(name = "transaction_kernel_id")
    private String transactionKernelId;

    @Column(name = "trace_id")
    private UUID traceId;

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    protected Vente() {
    }

    public Vente(UUID businessId, UUID clientId, UUID ordonnanceId, BigDecimal montantTotal, String devise,
                String modePaiement, String statutBcaas, String transactionKernelId, UUID traceId,
                UUID idempotencyKey) {
        this.businessId = businessId;
        this.clientId = clientId;
        this.ordonnanceId = ordonnanceId;
        this.montantTotal = montantTotal;
        this.devise = devise;
        this.modePaiement = modePaiement;
        this.statutBcaas = statutBcaas;
        this.transactionKernelId = transactionKernelId;
        this.traceId = traceId;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public UUID getBusinessId() { return businessId; }
    public UUID getClientId() { return clientId; }
    public UUID getOrdonnanceId() { return ordonnanceId; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public String getDevise() { return devise; }
    public String getModePaiement() { return modePaiement; }
    public String getStatutBcaas() { return statutBcaas; }
    public String getTransactionKernelId() { return transactionKernelId; }
    public UUID getTraceId() { return traceId; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreeLe() { return creeLe; }
}
