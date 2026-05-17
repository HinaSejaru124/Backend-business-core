package com.bcaas.core.infrastructure.persistence.resource;

import com.bcaas.core.resource.domain.model.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.AuditInfo;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("bcaas_resources")
public class ResourceEntity {

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("owner_id")
    private UUID ownerId;

    @Column("title")
    private String title;

    @Column("summary")
    private String summary;

    @Column("locale")
    private String locale;

    @Column("status")
    private String status;

    @Column("type")
    private String type;

    @Column("created_at")
    private Instant createdAt;

    @Column("created_by")
    private UUID createdBy;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("updated_by")
    private UUID updatedBy;

    public static ResourceEntity fromDomain(Resource resource) {
        ResourceContent content = resource.getContent();
        AuditInfo audit = resource.getAuditInfo();

        return ResourceEntity.builder()
                .id(resource.getId().value())
                .tenantId(resource.getTenantId().value())
                .ownerId(resource.getOwnerId().value())
                .title(content.title())
                .summary(content.summary())
                .locale(content.locale())
                .status(resource.getStatus().name())
                .type(resource.getType().name())
                .createdAt(audit.createdAt())
                .createdBy(audit.createdBy().value())
                .updatedAt(audit.updatedAt())
                .updatedBy(audit.updatedBy().value())
                .build();
    }

    public Resource toDomain() {
        ResourceContent content = ResourceContent.of(title, summary, locale);
        AuditInfo auditInfo = new AuditInfo(
                createdAt, ActorId.of(createdBy),
                updatedAt, ActorId.of(updatedBy)
        );
        return Resource.reconstitute(
                ResourceId.of(id), TenantId.of(tenantId),
                ActorId.of(ownerId), content,
                ResourceStatus.valueOf(status),
                ResourceType.valueOf(type),
                auditInfo
        );
    }
}
