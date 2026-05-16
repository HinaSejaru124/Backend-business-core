package com.bcaas.core.resource.domain.model;

import com.bcaas.core.resource.domain.event.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root du domaine Resource.
 *
 * Une Resource est toute entité métier créée et gérée par un Actor
 * dans le contexte d'un Tenant. BCaaS ne connaît pas la sémantique
 * métier du contenu — c'est l'application (BuaaS, transport...) qui
 * l'interprète.
 *
 * Règles métier :
 * - Une resource appartient à un tenant et un owner (Actor)
 * - Seul l'owner ou un ADMIN peut modifier une resource
 * - PUBLISHED → ARCHIVED est irréversible
 * - REJECTED → DRAFT permet une correction et re-soumission
 * - Une resource DRAFT n'est visible que par son owner
 */
public class Resource {

    private final ResourceId id;
    private final TenantId tenantId;
    private final ActorId ownerId;
    private ResourceContent content;
    private ResourceStatus status;
    private ResourceType type;
    private AuditInfo auditInfo;
    private final List<ResourceDomainEvent> domainEvents = new ArrayList<>();

    private Resource(ResourceId id, TenantId tenantId, ActorId ownerId,
                     ResourceContent content, ResourceStatus status,
                     ResourceType type, AuditInfo auditInfo) {
        this.id = id;
        this.tenantId = tenantId;
        this.ownerId = ownerId;
        this.content = content;
        this.status = status;
        this.type = type;
        this.auditInfo = auditInfo;
    }

    public static Resource create(TenantId tenantId, ActorId ownerId,
                                  ResourceContent content, ResourceType type) {
        ResourceId id = ResourceId.generate();
        Resource resource = new Resource(
                id, tenantId, ownerId, content,
                ResourceStatus.DRAFT, type,
                AuditInfo.create(ownerId)
        );
        resource.domainEvents.add(new ResourceCreatedEvent(id, tenantId, ownerId, type));
        return resource;
    }

    public static Resource reconstitute(ResourceId id, TenantId tenantId,
                                        ActorId ownerId, ResourceContent content,
                                        ResourceStatus status, ResourceType type,
                                        AuditInfo auditInfo) {
        return new Resource(id, tenantId, ownerId, content, status, type, auditInfo);
    }

    // ================================================================
    // Commandes métier
    // ================================================================

    public void updateContent(ResourceContent newContent, ActorId updatedBy) {
        assertCanModify(updatedBy);
        if (this.status == ResourceStatus.PUBLISHED || this.status == ResourceStatus.ARCHIVED) {
            throw new IllegalStateException(
                "Une ressource PUBLISHED ou ARCHIVED ne peut pas être modifiée directement");
        }
        this.content = newContent;
        this.auditInfo = auditInfo.update(updatedBy);
        this.domainEvents.add(new ResourceUpdatedEvent(this.id, this.tenantId));
    }

    public void submitForReview(ActorId submittedBy) {
        assertCanModify(submittedBy);
        if (this.status != ResourceStatus.DRAFT && this.status != ResourceStatus.REJECTED) {
            throw new IllegalStateException(
                "Seule une ressource DRAFT ou REJECTED peut être soumise en revue");
        }
        this.status = ResourceStatus.PENDING_REVIEW;
        this.auditInfo = auditInfo.update(submittedBy);
        this.domainEvents.add(new ResourceSubmittedEvent(this.id, this.tenantId));
    }

    public void publish(ActorId publishedBy) {
        if (this.status != ResourceStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                "Seule une ressource PENDING_REVIEW peut être publiée");
        }
        this.status = ResourceStatus.PUBLISHED;
        this.auditInfo = auditInfo.update(publishedBy);
        this.domainEvents.add(new ResourcePublishedEvent(this.id, this.tenantId, this.ownerId));
    }

    public void reject(ActorId rejectedBy, String reason) {
        if (this.status != ResourceStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                "Seule une ressource PENDING_REVIEW peut être rejetée");
        }
        this.status = ResourceStatus.REJECTED;
        this.auditInfo = auditInfo.update(rejectedBy);
        this.domainEvents.add(new ResourceRejectedEvent(this.id, this.tenantId, reason));
    }

    public void archive(ActorId archivedBy) {
        if (this.status != ResourceStatus.PUBLISHED) {
            throw new IllegalStateException(
                "Seule une ressource PUBLISHED peut être archivée");
        }
        this.status = ResourceStatus.ARCHIVED;
        this.auditInfo = auditInfo.update(archivedBy);
        this.domainEvents.add(new ResourceArchivedEvent(this.id, this.tenantId));
    }

    // ================================================================
    // Règles métier (queries)
    // ================================================================

    public boolean isPublic() { return this.status == ResourceStatus.PUBLISHED; }

    public boolean isOwnedBy(ActorId actorId) { return this.ownerId.equals(actorId); }

    public boolean isEditableBy(ActorId actorId) {
        return isOwnedBy(actorId) &&
               (status == ResourceStatus.DRAFT || status == ResourceStatus.REJECTED);
    }

    private void assertCanModify(ActorId actorId) {
        if (!isOwnedBy(actorId)) {
            throw new IllegalStateException(
                "Seul l'owner peut modifier cette ressource");
        }
    }

    // ================================================================
    // Événements & Getters
    // ================================================================

    public List<ResourceDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() { domainEvents.clear(); }

    public ResourceId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public ActorId getOwnerId() { return ownerId; }
    public ResourceContent getContent() { return content; }
    public ResourceStatus getStatus() { return status; }
    public ResourceType getType() { return type; }
    public AuditInfo getAuditInfo() { return auditInfo; }
}
