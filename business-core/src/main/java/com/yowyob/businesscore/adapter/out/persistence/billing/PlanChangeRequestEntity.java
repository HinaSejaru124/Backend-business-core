package com.yowyob.businesscore.adapter.out.persistence.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'audit d'une demande de changement de plan (upgrade/downgrade) et de son issue de paiement.
 * Table sans RLS (rattachée au compte développeur, comme {@code developer_account} / {@code api_key}).
 * Support de la réconciliation future des paiements EN_ATTENTE quand l'API Kernel sera disponible.
 */
@Table("plan_change_request")
public class PlanChangeRequestEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("developer_id")
    private UUID developerId;

    @Column("plan_from")
    private String planFrom;

    @Column("plan_to")
    private String planTo;

    private String statut;

    @Column("payment_reference")
    private String paymentReference;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean nouveau;

    public PlanChangeRequestEntity() {
    }

    public static PlanChangeRequestEntity nouveau(UUID developerId, String planFrom, String planTo,
                                                  String statut, String paymentReference) {
        PlanChangeRequestEntity e = new PlanChangeRequestEntity();
        e.id = UUID.randomUUID();
        e.developerId = developerId;
        e.planFrom = planFrom;
        e.planTo = planTo;
        e.statut = statut;
        e.paymentReference = paymentReference;
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

    public String getPlanFrom() {
        return planFrom;
    }

    public String getPlanTo() {
        return planTo;
    }

    public String getStatut() {
        return statut;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
