package com.bcaas.core.actor.domain.model;

import com.bcaas.core.actor.domain.event.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.TenantId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root du domaine Actor.
 *
 * Un Actor est toute entité humaine ou système interagissant
 * avec la plateforme BCaaS dans le contexte d'un tenant.
 *
 * Règles métier :
 * - Un acteur appartient à exactement un tenant (isolation multi-tenant)
 * - Un acteur PENDING doit vérifier son identité avant d'être actif
 * - Un acteur SUSPENDED ne peut pas créer de ressources
 * - L'email est unique par tenant (pas globalement)
 * - Un acteur ne peut avoir qu'un seul rôle principal par tenant
 */
public class Actor {

    private final ActorId id;
    private final TenantId tenantId;
    private ActorIdentity identity;
    private ActorRole role;
    private ActorStatus status;
    private ActorProfile profile;
    private AuditInfo auditInfo;
    private final List<ActorDomainEvent> domainEvents = new ArrayList<>();

    private Actor(
            ActorId id,
            TenantId tenantId,
            ActorIdentity identity,
            ActorRole role,
            ActorStatus status,
            ActorProfile profile,
            AuditInfo auditInfo
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.identity = identity;
        this.role = role;
        this.status = status;
        this.profile = profile;
        this.auditInfo = auditInfo;
    }

    public static Actor create(
            TenantId tenantId,
            ActorIdentity identity,
            ActorRole role,
            ActorId createdBy
    ) {
        ActorId id = ActorId.generate();
        Actor actor = new Actor(
                id, tenantId, identity, role,
                ActorStatus.PENDING_VERIFICATION,
                ActorProfile.empty(),
                AuditInfo.create(createdBy)
        );
        actor.domainEvents.add(new ActorCreatedEvent(id, tenantId, identity.email(), role));
        return actor;
    }

    public static Actor reconstitute(
            ActorId id,
            TenantId tenantId,
            ActorIdentity identity,
            ActorRole role,
            ActorStatus status,
            ActorProfile profile,
            AuditInfo auditInfo
    ) {
        return new Actor(id, tenantId, identity, role, status, profile, auditInfo);
    }

    // ================================================================
    // Commandes métier
    // ================================================================

    public void verify(ActorId verifiedBy) {
        if (this.status != ActorStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException(
                "Seul un acteur PENDING_VERIFICATION peut être vérifié"
            );
        }
        this.status = ActorStatus.ACTIVE;
        this.auditInfo = auditInfo.update(verifiedBy);
        this.domainEvents.add(new ActorVerifiedEvent(this.id, this.tenantId));
    }

    public void suspend(ActorId suspendedBy, String reason) {
        if (this.status != ActorStatus.ACTIVE) {
            throw new IllegalStateException("Seul un acteur ACTIVE peut être suspendu");
        }
        this.status = ActorStatus.SUSPENDED;
        this.auditInfo = auditInfo.update(suspendedBy);
        this.domainEvents.add(new ActorSuspendedEvent(this.id, this.tenantId, reason));
    }

    public void reactivate(ActorId reactivatedBy) {
        if (this.status != ActorStatus.SUSPENDED) {
            throw new IllegalStateException(
                "Seul un acteur SUSPENDED peut être réactivé"
            );
        }
        this.status = ActorStatus.ACTIVE;
        this.auditInfo = auditInfo.update(reactivatedBy);
    }

    public void deactivate(ActorId deactivatedBy) {
        this.status = ActorStatus.DEACTIVATED;
        this.auditInfo = auditInfo.update(deactivatedBy);
        this.domainEvents.add(new ActorDeactivatedEvent(this.id, this.tenantId));
    }

    public void updateProfile(ActorProfile newProfile, ActorId updatedBy) {
        this.profile = newProfile;
        this.auditInfo = auditInfo.update(updatedBy);
    }

    public void changeRole(ActorRole newRole, ActorId changedBy) {
        if (newRole == this.role) {
            throw new IllegalArgumentException("Le rôle est déjà " + newRole);
        }
        ActorRole oldRole = this.role;
        this.role = newRole;
        this.auditInfo = auditInfo.update(changedBy);
        this.domainEvents.add(new ActorRoleChangedEvent(this.id, this.tenantId, oldRole, newRole));
    }

    // ================================================================
    // Règles métier (queries)
    // ================================================================

    public boolean isActive() { return this.status == ActorStatus.ACTIVE; }

    public boolean canCreateResource() { return isActive(); }

    public boolean canModerateContent() {
        return isActive() && (role == ActorRole.ADMIN || role == ActorRole.OWNER);
    }

    public boolean belongsTo(TenantId tenantId) {
        return this.tenantId.equals(tenantId);
    }

    // ================================================================
    // Événements & Getters
    // ================================================================

    public List<ActorDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() { domainEvents.clear(); }

    public ActorId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public ActorIdentity getIdentity() { return identity; }
    public ActorRole getRole() { return role; }
    public ActorStatus getStatus() { return status; }
    public ActorProfile getProfile() { return profile; }
    public AuditInfo getAuditInfo() { return auditInfo; }
}
