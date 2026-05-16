package com.bcaas.core.tenant.domain.model;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.event.TenantActivatedEvent;
import com.bcaas.core.tenant.domain.event.TenantCreatedEvent;
import com.bcaas.core.tenant.domain.event.TenantDomainEvent;
import com.bcaas.core.tenant.domain.event.TenantSuspendedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root du domaine Tenant.
 *
 * Un Tenant représente un client isolé de la plateforme BCaaS.
 * Analogie réseau : un VLAN — isolation logique sur infrastructure partagée.
 *
 * Règles métier encapsulées :
 * - Un tenant PENDING doit être activé avant d'utiliser le système
 * - Un tenant SUSPENDED ne peut pas créer de nouvelles ressources
 * - Un tenant DEACTIVATED est définitif — pas de réactivation
 * - Le plan détermine les limites d'utilisation (acteurs, ressources)
 */
public class Tenant {

    private final TenantId id;
    private String name;
    private String slug;
    private TenantStatus status;
    private TenantPlan plan;
    private TenantSettings settings;
    private AuditInfo auditInfo;

    // Événements domaine accumulés (pattern Outbox)
    private final List<TenantDomainEvent> domainEvents = new ArrayList<>();

    // Constructeur privé — création uniquement via factory methods
    private Tenant(
            TenantId id,
            String name,
            String slug,
            TenantStatus status,
            TenantPlan plan,
            TenantSettings settings,
            AuditInfo auditInfo
    ) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.status = status;
        this.plan = plan;
        this.settings = settings;
        this.auditInfo = auditInfo;
    }

    /**
     * Crée un nouveau tenant en état PENDING.
     * La validation et l'activation sont des étapes séparées.
     */
    public static Tenant create(
            String name,
            String slug,
            TenantPlan plan,
            TenantSettings settings,
            ActorId createdBy
    ) {
        validateName(name);
        validateSlug(slug);

        TenantId id = TenantId.generate();
        Tenant tenant = new Tenant(
                id,
                name,
                slug,
                TenantStatus.PENDING,
                plan,
                settings,
                AuditInfo.create(createdBy)
        );

        tenant.domainEvents.add(new TenantCreatedEvent(id, name, slug, plan));
        return tenant;
    }

    /**
     * Reconstitue un tenant depuis la persistance.
     * Pas d'événements — c'est une reconstruction, pas une création.
     */
    public static Tenant reconstitute(
            TenantId id,
            String name,
            String slug,
            TenantStatus status,
            TenantPlan plan,
            TenantSettings settings,
            AuditInfo auditInfo
    ) {
        return new Tenant(id, name, slug, status, plan, settings, auditInfo);
    }

    // ================================================================
    // Commandes métier
    // ================================================================

    public void activate(ActorId activatedBy) {
        if (this.status == TenantStatus.DEACTIVATED) {
            throw new IllegalStateException(
                "Un tenant désactivé ne peut pas être réactivé"
            );
        }
        this.status = TenantStatus.ACTIVE;
        this.auditInfo = auditInfo.update(activatedBy);
        this.domainEvents.add(new TenantActivatedEvent(this.id));
    }

    public void suspend(ActorId suspendedBy, String reason) {
        if (this.status != TenantStatus.ACTIVE) {
            throw new IllegalStateException(
                "Seul un tenant ACTIVE peut être suspendu"
            );
        }
        this.status = TenantStatus.SUSPENDED;
        this.auditInfo = auditInfo.update(suspendedBy);
        this.domainEvents.add(new TenantSuspendedEvent(this.id, reason));
    }

    public void deactivate(ActorId deactivatedBy) {
        this.status = TenantStatus.DEACTIVATED;
        this.auditInfo = auditInfo.update(deactivatedBy);
    }

    public void upgradePlan(TenantPlan newPlan, ActorId updatedBy) {
        if (newPlan.ordinal() <= this.plan.ordinal()) {
            throw new IllegalArgumentException(
                "Le nouveau plan doit être supérieur au plan actuel"
            );
        }
        this.plan = newPlan;
        this.auditInfo = auditInfo.update(updatedBy);
    }

    public void updateSettings(TenantSettings newSettings, ActorId updatedBy) {
        this.settings = newSettings;
        this.auditInfo = auditInfo.update(updatedBy);
    }

    // ================================================================
    // Règles métier (queries)
    // ================================================================

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    public boolean canCreateActor(int currentActorCount) {
        return isActive() && currentActorCount < plan.getMaxActors();
    }

    public boolean canCreateResource(int currentMonthlyCount) {
        return isActive() && currentMonthlyCount < plan.getMaxResourcesPerMonth();
    }

    // ================================================================
    // Validations
    // ================================================================

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom du tenant est obligatoire");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException(
                "Le nom du tenant doit contenir entre 2 et 100 caractères"
            );
        }
    }

    private static void validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Le slug du tenant est obligatoire");
        }
        if (!slug.matches("^[a-z0-9-]{2,50}$")) {
            throw new IllegalArgumentException(
                "Le slug doit contenir uniquement des lettres minuscules, chiffres et tirets (2-50 caractères)"
            );
        }
    }

    // ================================================================
    // Événements domaine
    // ================================================================

    public List<TenantDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ================================================================
    // Getters (pas de setters — immutabilité contrôlée)
    // ================================================================

    public TenantId getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public TenantStatus getStatus() { return status; }
    public TenantPlan getPlan() { return plan; }
    public TenantSettings getSettings() { return settings; }
    public AuditInfo getAuditInfo() { return auditInfo; }
}
