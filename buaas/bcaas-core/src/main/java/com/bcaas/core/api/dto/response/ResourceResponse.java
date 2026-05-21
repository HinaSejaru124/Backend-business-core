package com.bcaas.core.api.dto.response;

import com.bcaas.core.resource.domain.model.Resource;
import java.time.Instant;
import java.util.Map;

public record ResourceResponse(
        String id,
        String tenantId,
        String ownerId,
        String title,
        String summary,
        String locale,
        String status,
        String type,
        Map<String, String> fields,
        Instant createdAt,
        Instant updatedAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId().toString(),
                resource.getTenantId().toString(),
                resource.getOwnerId().toString(),
                resource.getContent().title(),
                resource.getContent().summary(),
                resource.getContent().locale(),
                resource.getStatus().name(),
                resource.getType().name(),
                resource.getContent().fields(),
                resource.getAuditInfo().createdAt(),
                resource.getAuditInfo().updatedAt()
        );
    }
}
