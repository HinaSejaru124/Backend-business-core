package com.bcaas.core.resource.port.input;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.resource.domain.model.Resource;
import com.bcaas.core.resource.domain.model.ResourceContent;
import com.bcaas.core.resource.domain.model.ResourceStatus;
import com.bcaas.core.resource.domain.model.ResourceType;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResourceUseCase {
    Mono<Resource> createResource(TenantId tenantId, ResourceContent content,
                                  ResourceType type, BusinessContext context);
    Mono<Resource> updateContent(ResourceId resourceId, ResourceContent content,
                                 BusinessContext context);
    Mono<Resource> submitForReview(ResourceId resourceId, BusinessContext context);
    Mono<Resource> publish(ResourceId resourceId, BusinessContext context);
    Mono<Resource> reject(ResourceId resourceId, String reason, BusinessContext context);
    Mono<Resource> archive(ResourceId resourceId, BusinessContext context);
    Mono<Resource> findById(ResourceId resourceId);
    Flux<Resource> findPublished(TenantId tenantId);
}
