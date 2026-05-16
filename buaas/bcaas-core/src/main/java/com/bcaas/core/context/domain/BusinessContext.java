package com.bcaas.core.context.domain;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;

import java.time.Instant;
import java.util.UUID;

/**
 * Le "paquet métier" — inspiré des en-têtes de protocoles réseau.
 *
 * Chaque requête traversant le système BCaaS est enrichie de ce contexte.
 * Il sépare les métadonnées d'acheminement du payload métier réel,
 * exactement comme les en-têtes TCP/IP séparent le routage des données.
 *
 * Analogie réseau :
 * - traceId      → adresse source (traçabilité bout-en-bout)
 * - correlationId → numéro de session (corrélation entre services)
 * - tenantId     → adresse IP destination (routage vers le bon tenant)
 * - actorId      → identité de l'émetteur
 * - roleScope    → permissions de l'émetteur
 * - locale       → localisation (langue, devise)
 * - policyLevel  → niveau de SLA / QoS
 */
public record BusinessContext(
        String traceId,
        String correlationId,
        TenantId tenantId,
        ActorId actorId,
        String roleScope,
        String locale,
        PolicyLevel policyLevel,
        Instant timestamp
) {
    public static BusinessContext of(
            TenantId tenantId,
            ActorId actorId,
            String roleScope,
            String locale,
            PolicyLevel policyLevel
    ) {
        return new BusinessContext(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                tenantId,
                actorId,
                roleScope,
                locale,
                policyLevel,
                Instant.now()
        );
    }

    public static BusinessContext system(TenantId tenantId) {
        return of(
                tenantId,
                ActorId.of(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                "SYSTEM",
                "fr",
                PolicyLevel.STANDARD
        );
    }

    public boolean isSystem() {
        return "SYSTEM".equals(roleScope);
    }
}
