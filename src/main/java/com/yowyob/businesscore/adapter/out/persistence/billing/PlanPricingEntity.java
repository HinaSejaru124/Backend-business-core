package com.yowyob.businesscore.adapter.out.persistence.billing;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Tarification d'un plan, éditée par l'administrateur (source de vérité persistée). {@code code} est la
 * clé primaire (nom du plan) : on contrôle l'identifiant, d'où {@link Persistable} avec drapeau nouveau.
 */
@Table("plan_pricing")
public class PlanPricingEntity implements Persistable<String> {

    @Id
    private String code;

    @Column("quota_mensuel")
    private long quotaMensuel;

    @Column("prix_mensuel")
    private long prixMensuel;

    private String devise;

    @Column("maj_le")
    private Instant majLe;

    @Transient
    private boolean nouveau;

    public PlanPricingEntity() {
    }

    public static PlanPricingEntity de(String code, long quotaMensuel, long prixMensuel, String devise, boolean nouveau) {
        PlanPricingEntity e = new PlanPricingEntity();
        e.code = code;
        e.quotaMensuel = quotaMensuel;
        e.prixMensuel = prixMensuel;
        e.devise = devise;
        e.majLe = Instant.now();
        e.nouveau = nouveau;
        return e;
    }

    @Override
    public String getId() {
        return code;
    }

    @Override
    public boolean isNew() {
        return nouveau;
    }

    public String getCode() {
        return code;
    }

    public long getQuotaMensuel() {
        return quotaMensuel;
    }

    public long getPrixMensuel() {
        return prixMensuel;
    }

    public String getDevise() {
        return devise;
    }

    public Instant getMajLe() {
        return majLe;
    }
}
