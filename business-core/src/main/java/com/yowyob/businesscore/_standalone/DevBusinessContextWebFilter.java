package com.yowyob.businesscore._standalone;

import com.yowyob.businesscore.shared.context.BusinessContext;
import com.yowyob.businesscore.shared.context.BusinessContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ⚠️ STANDALONE ONLY — remplace la Barrière 1 du socle (BusinessContextWebFilter).
 * Pose un BusinessContext dans le contexte réactif à partir d'en-têtes, avec un tenant de dev par défaut.
 * À SUPPRIMER lors de l'intégration au socle (qui fournit le vrai filtre + le SET app.current_tenant).
 */
@Component
@Order(-100)
public class DevBusinessContextWebFilter implements WebFilter {

    private static final UUID DEV_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DEV_ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders h = exchange.getRequest().getHeaders();
        UUID tenant = parse(h.getFirst("X-Tenant-Id"), DEV_TENANT);
        UUID actor = parse(h.getFirst("X-Actor-Id"), DEV_ACTOR);
        List<String> roles = h.getOrEmpty("X-Roles");
        BusinessContext bc = new BusinessContext(
                tenant, actor, roles, null, exchange.getRequest().getId(), Locale.FRENCH);
        return chain.filter(exchange).contextWrite(ctx -> BusinessContextHolder.with(ctx, bc));
    }

    private static UUID parse(String value, UUID fallback) {
        try {
            return value == null || value.isBlank() ? fallback : UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
