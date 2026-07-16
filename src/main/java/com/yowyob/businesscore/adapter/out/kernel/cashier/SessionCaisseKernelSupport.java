package com.yowyob.businesscore.adapter.out.kernel.cashier;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.internal.ContexteKernel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Résout une session de caisse ouverte pour la caisse courante, ou en ouvre une si nécessaire
 * ({@code GET /api/cashier/sessions}, {@code POST /api/sessions}). Sans caisse ou caissier connus,
 * renvoie vide : le paiement part alors sans {@code sessionId} (session par défaut côté kernel).
 */
@Component
class SessionCaisseKernelSupport {

    private final KernelClient kernel;

    SessionCaisseKernelSupport(KernelClient kernel) {
        this.kernel = kernel;
    }

    Mono<Optional<UUID>> resoudreOuOuvrir(ContexteKernel ctx) {
        if (ctx.registerId() == null) {
            return Mono.just(Optional.empty());
        }
        return kernel.getForOrganization("/api/cashier/sessions", CashierSessionView[].class,
                        ctx.organizationId())
                .flatMap(sessions -> {
                    for (CashierSessionView session : sessions) {
                        if (session.estOuverte(ctx.registerId())) {
                            return Mono.just(Optional.of(session.id()));
                        }
                    }
                    return ouvrirSession(ctx);
                })
                .switchIfEmpty(ouvrirSession(ctx));
    }

    private Mono<Optional<UUID>> ouvrirSession(ContexteKernel ctx) {
        if (ctx.cashierId() == null) {
            return Mono.just(Optional.empty());
        }
        CreateSessionRequest requete = new CreateSessionRequest(
                ctx.registerId(), ctx.cashierId(), BigDecimal.ZERO, ctx.currency());
        return kernel.postForOrganization("/api/sessions", requete, CashierSessionView.class,
                        ctx.organizationId())
                .map(session -> Optional.of(session.id()))
                .defaultIfEmpty(Optional.empty());
    }
}

record CashierSessionView(UUID id, UUID registerId, String status) {

    boolean estOuverte(UUID registerId) {
        return registerId != null && registerId.equals(this.registerId)
                && status != null && "OPEN".equalsIgnoreCase(status);
    }
}

record CreateSessionRequest(UUID registerId, UUID cashierId, BigDecimal openingAmount, String currency) {
}
