package com.bcaas.core.api.filter;

import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.context.domain.PolicyLevel;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtre WebFlux qui construit le BusinessContext à partir des headers HTTP.
 *
 * Analogie réseau : couche réseau qui lit les en-têtes IP/TCP
 * pour router et enrichir le contexte de chaque requête.
 *
 * Headers attendus :
 * - X-Tenant-ID    : identifiant du tenant (couche 3 — Tenant & Routing)
 * - X-Actor-ID     : identifiant de l'acteur (couche 4 — Context & Policy)
 * - X-Role-Scope   : rôle de l'acteur
 * - X-Locale       : langue préférée
 * - X-Trace-ID     : identifiant de traçabilité bout-en-bout
 * - X-Policy-Level : niveau de SLA
 */
@Component
public class BusinessContextFilter implements WebFilter {

    public static final String CONTEXT_ATTRIBUTE = "BUSINESS_CONTEXT";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String tenantIdHeader = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        String actorIdHeader = exchange.getRequest().getHeaders().getFirst("X-Actor-ID");
        String roleScope = exchange.getRequest().getHeaders().getFirst("X-Role-Scope");
        String locale = exchange.getRequest().getHeaders().getFirst("X-Locale");
        String policyLevelHeader = exchange.getRequest().getHeaders().getFirst("X-Policy-Level");

        if (tenantIdHeader != null && actorIdHeader != null) {
            try {
                TenantId tenantId = TenantId.of(tenantIdHeader);
                ActorId actorId = ActorId.of(actorIdHeader);
                PolicyLevel policyLevel = parsePolicyLevel(policyLevelHeader);

                BusinessContext context = BusinessContext.of(
                        tenantId, actorId,
                        roleScope != null ? roleScope : "CONSUMER",
                        locale != null ? locale : "fr",
                        policyLevel
                );

                exchange.getAttributes().put(CONTEXT_ATTRIBUTE, context);
            } catch (IllegalArgumentException ignored) {
                // Headers invalides — le controller gérera l'erreur
            }
        }

        return chain.filter(exchange);
    }

    private PolicyLevel parsePolicyLevel(String value) {
        if (value == null) return PolicyLevel.STANDARD;
        try {
            return PolicyLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PolicyLevel.STANDARD;
        }
    }
}
