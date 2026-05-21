package com.bcaas.core.api.rest.actor;

import com.bcaas.core.actor.domain.model.ActorIdentity;
import com.bcaas.core.actor.domain.model.ActorProfile;
import com.bcaas.core.actor.domain.model.ActorRole;
import com.bcaas.core.actor.port.input.ActorUseCase;
import com.bcaas.core.api.dto.request.CreateActorRequest;
import com.bcaas.core.api.dto.request.SuspendRequest;
import com.bcaas.core.api.dto.response.ActorResponse;
import com.bcaas.core.api.filter.BusinessContextFilter;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.shared.domain.ActorId;
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
@RequestMapping("/api/v1/actors")
@Tag(name = "Actors", description = "Gestion des acteurs — Couche 5 : Business Capabilities")
public class ActorController {

    private final ActorUseCase actorUseCase;

    public ActorController(ActorUseCase actorUseCase) {
        this.actorUseCase = actorUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un acteur dans un tenant")
    public Mono<ResponseEntity<ActorResponse>> createActor(
            @Valid @RequestBody CreateActorRequest request,
            ServerWebExchange exchange) {

        BusinessContext context = getContext(exchange);
        ActorIdentity identity = new ActorIdentity(
                request.email(), request.firstName(), request.lastName(),
                request.phoneNumber(), request.locale()
        );

        return actorUseCase.createActor(context.tenantId(), identity, request.role(), context)
                .map(actor -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ActorResponse.from(actor)));
    }

    @GetMapping("/{actorId}")
    @Operation(summary = "Récupérer un acteur par ID")
    public Mono<ResponseEntity<ActorResponse>> findById(@PathVariable String actorId) {
        return actorUseCase.findById(ActorId.of(actorId))
                .map(actor -> ResponseEntity.ok(ActorResponse.from(actor)));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Lister tous les acteurs d'un tenant")
    public Flux<ActorResponse> findAllByTenant(@PathVariable String tenantId) {
        return actorUseCase.findAllByTenant(TenantId.of(tenantId))
                .map(ActorResponse::from);
    }

    @PutMapping("/{actorId}/verify")
    @Operation(summary = "Vérifier l'identité d'un acteur")
    public Mono<ResponseEntity<ActorResponse>> verify(
            @PathVariable String actorId,
            ServerWebExchange exchange) {
        return actorUseCase.verifyActor(ActorId.of(actorId), getContext(exchange))
                .map(actor -> ResponseEntity.ok(ActorResponse.from(actor)));
    }

    @PutMapping("/{actorId}/suspend")
    @Operation(summary = "Suspendre un acteur")
    public Mono<ResponseEntity<ActorResponse>> suspend(
            @PathVariable String actorId,
            @Valid @RequestBody SuspendRequest request,
            ServerWebExchange exchange) {
        return actorUseCase.suspendActor(ActorId.of(actorId), request.reason(), getContext(exchange))
                .map(actor -> ResponseEntity.ok(ActorResponse.from(actor)));
    }

    @PutMapping("/{actorId}/reactivate")
    @Operation(summary = "Réactiver un acteur suspendu")
    public Mono<ResponseEntity<ActorResponse>> reactivate(
            @PathVariable String actorId,
            ServerWebExchange exchange) {
        return actorUseCase.reactivateActor(ActorId.of(actorId), getContext(exchange))
                .map(actor -> ResponseEntity.ok(ActorResponse.from(actor)));
    }

    @PutMapping("/{actorId}/role")
    @Operation(summary = "Changer le rôle d'un acteur")
    public Mono<ResponseEntity<ActorResponse>> changeRole(
            @PathVariable String actorId,
            @RequestParam ActorRole role,
            ServerWebExchange exchange) {
        return actorUseCase.changeRole(ActorId.of(actorId), role, getContext(exchange))
                .map(actor -> ResponseEntity.ok(ActorResponse.from(actor)));
    }

    private BusinessContext getContext(ServerWebExchange exchange) {
        BusinessContext context = (BusinessContext) exchange.getAttributes()
                .get(BusinessContextFilter.CONTEXT_ATTRIBUTE);
        if (context == null) {
            throw new SecurityException("BusinessContext manquant");
        }
        return context;
    }
}
