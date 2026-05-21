package com.bcaas.core.api.rest.resource;

import com.bcaas.core.api.dto.request.CreateResourceRequest;
import com.bcaas.core.api.dto.request.RejectResourceRequest;
import com.bcaas.core.api.dto.request.UpdateResourceRequest;
import com.bcaas.core.api.dto.response.ResourceResponse;
import com.bcaas.core.api.filter.BusinessContextFilter;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.resource.domain.model.ResourceContent;
import com.bcaas.core.resource.port.input.ResourceUseCase;
import com.bcaas.core.shared.domain.ResourceId;
import com.bcaas.core.shared.domain.TenantId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/resources")
@Tag(name = "Resources", description = "Gestion des ressources génériques — Couche 5 : Business Capabilities")
public class ResourceController {

    private final ResourceUseCase resourceUseCase;

    public ResourceController(ResourceUseCase resourceUseCase) {
        this.resourceUseCase = resourceUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une ressource")
    public Mono<ResponseEntity<ResourceResponse>> create(
            @Valid @RequestBody CreateResourceRequest request,
            ServerWebExchange exchange) {

        BusinessContext context = getContext(exchange);
        ResourceContent content = new ResourceContent(
                request.title(), request.summary(), request.fields(), request.locale()
        );

        return resourceUseCase.createResource(
                context.tenantId(), content, request.type(), context)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ResourceResponse.from(r)));
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Récupérer une ressource par ID")
    public Mono<ResponseEntity<ResourceResponse>> findById(@PathVariable String resourceId) {
        return resourceUseCase.findById(ResourceId.of(resourceId))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    @GetMapping("/tenant/{tenantId}/published")
    @Operation(summary = "Lister les ressources publiées d'un tenant")
    public Flux<ResourceResponse> findPublished(@PathVariable String tenantId) {
        return resourceUseCase.findPublished(TenantId.of(tenantId))
                .map(ResourceResponse::from);
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Mettre à jour le contenu d'une ressource")
    public Mono<ResponseEntity<ResourceResponse>> update(
            @PathVariable String resourceId,
            @Valid @RequestBody UpdateResourceRequest request,
            ServerWebExchange exchange) {

        ResourceContent content = new ResourceContent(
                request.title(), request.summary(), request.fields(), request.locale()
        );

        return resourceUseCase.updateContent(ResourceId.of(resourceId), content, getContext(exchange))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    @PutMapping("/{resourceId}/submit")
    @Operation(summary = "Soumettre une ressource pour révision")
    public Mono<ResponseEntity<ResourceResponse>> submit(
            @PathVariable String resourceId,
            ServerWebExchange exchange) {
        return resourceUseCase.submitForReview(ResourceId.of(resourceId), getContext(exchange))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    @PutMapping("/{resourceId}/publish")
    @Operation(summary = "Publier une ressource")
    public Mono<ResponseEntity<ResourceResponse>> publish(
            @PathVariable String resourceId,
            ServerWebExchange exchange) {
        return resourceUseCase.publish(ResourceId.of(resourceId), getContext(exchange))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    @PutMapping("/{resourceId}/reject")
    @Operation(summary = "Rejeter une ressource")
    public Mono<ResponseEntity<ResourceResponse>> reject(
            @PathVariable String resourceId,
            @Valid @RequestBody RejectResourceRequest request,
            ServerWebExchange exchange) {
        return resourceUseCase.reject(ResourceId.of(resourceId), request.reason(), getContext(exchange))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    @PutMapping("/{resourceId}/archive")
    @Operation(summary = "Archiver une ressource publiée")
    public Mono<ResponseEntity<ResourceResponse>> archive(
            @PathVariable String resourceId,
            ServerWebExchange exchange) {
        return resourceUseCase.archive(ResourceId.of(resourceId), getContext(exchange))
                .map(r -> ResponseEntity.ok(ResourceResponse.from(r)));
    }

    private BusinessContext getContext(ServerWebExchange exchange) {
        BusinessContext context = (BusinessContext) exchange.getAttributes()
                .get(BusinessContextFilter.CONTEXT_ATTRIBUTE);
        if (context == null) throw new SecurityException("BusinessContext manquant");
        return context;
    }
}
