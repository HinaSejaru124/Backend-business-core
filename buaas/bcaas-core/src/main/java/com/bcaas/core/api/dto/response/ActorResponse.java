package com.bcaas.core.api.dto.response;

import com.bcaas.core.actor.domain.model.Actor;
import java.time.Instant;

public record ActorResponse(
        String id,
        String tenantId,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String locale,
        String role,
        String status,
        String bio,
        String avatarUrl,
        String location,
        Instant createdAt
) {
    public static ActorResponse from(Actor actor) {
        return new ActorResponse(
                actor.getId().toString(),
                actor.getTenantId().toString(),
                actor.getIdentity().email(),
                actor.getIdentity().firstName(),
                actor.getIdentity().lastName(),
                actor.getIdentity().phoneNumber(),
                actor.getIdentity().locale(),
                actor.getRole().name(),
                actor.getStatus().name(),
                actor.getProfile().bio(),
                actor.getProfile().avatarUrl(),
                actor.getProfile().location(),
                actor.getAuditInfo().createdAt()
        );
    }
}
