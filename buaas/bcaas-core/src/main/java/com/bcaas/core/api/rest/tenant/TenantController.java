package com.bcaas.core.api.rest.tenant;

import com.bcaas.core.api.dto.request.CreateTenantRequest;
import com.bcaas.core.api.dto.request.SuspendRequest;
import com.bcaas.core.api.dto.response.TenantResponse;
import com.bcaas.core.api.filter.BusinessContextFilter;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.TenantPlan;
import com.bcaas.core.tenant.domain.model.TenantSettings;
import com.bcaas.core.tenant.port.input.TenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller REST pour la gestion des Tenants.
 * Couche API — Adapteur entrant de l'architecture hexagonale.
 *
 * Analogie réseau : point d'entrée du réseau (gateway).
 * Reçoit les requêtes HTTP, construit le contexte et délègue au domaine.
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Gestion multi-tenant — Couche 3 : Tenant & Routing")
public class TenantController {

    private final TenantUseCase tenantUseCase;

    public TenantController(TenantUseCase tenantUseCase) {
        this.tenantUseCase = tenantUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un nouveau tenant")
    public Mono<ResponseEntity<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            ServerWebExchange exchange) {

        BusinessContext context = getContext(exchange);

        TenantSettings settings = new TenantSettings(
                request.defaultLocale(),
                request.defaultCurrency(),
                request.timezone(),
                true, true, 60
        );

        return tenantUseCase.createTenant(
                request.name(), request.slug(),
                request.plan(), settings, context
        )
        .map(tenant -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TenantResponse.from(tenant)));
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Récupérer un tenant par ID")
    public Mono<ResponseEntity<TenantResponse>> findById(
            @PathVariable String tenantId) {
        return tenantUseCase.findById(TenantId.of(tenantId))
                .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Récupérer un tenant par slug")
    public Mono<ResponseEntity<TenantResponse>> findBySlug(
            @PathVariable String slug) {
        return tenantUseCase.findBySlug(slug)
                .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)));
    }

    @PutMapping("/{tenantId}/activate")
    @Operation(summary = "Activer un tenant")
    public Mono<ResponseEntity<TenantResponse>> activate(
            @PathVariable String tenantId,
            ServerWebExchange exchange) {
        return tenantUseCase.activateTenant(TenantId.of(tenantId), getContext(exchange))
                .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)));
    }

    @PutMapping("/{tenantId}/suspend")
    @Operation(summary = "Suspendre un tenant")
    public Mono<ResponseEntity<TenantResponse>> suspend(
            @PathVariable String tenantId,
            @Valid @RequestBody SuspendRequest request,
            ServerWebExchange exchange) {
        return tenantUseCase.suspendTenant(
                TenantId.of(tenantId), request.reason(), getContext(exchange))
                .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)));
    }

    @PutMapping("/{tenantId}/upgrade")
    @Operation(summary = "Upgrader le plan d'un tenant")
    public Mono<ResponseEntity<TenantResponse>> upgradePlan(
            @PathVariable String tenantId,
            @RequestParam TenantPlan plan,
            ServerWebExchange exchange) {
        return tenantUseCase.upgradePlan(
                TenantId.of(tenantId), plan, getContext(exchange))
                .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)));
    }

    private BusinessContext getContext(ServerWebExchange exchange) {
        BusinessContext context = (BusinessContext) exchange.getAttributes()
                .get(BusinessContextFilter.CONTEXT_ATTRIBUTE);
        if (context == null) {
            throw new SecurityException("BusinessContext manquant — headers X-Tenant-ID et X-Actor-ID requis");
        }
        return context;
    }
}
