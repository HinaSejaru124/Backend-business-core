package com.yowyob.businesscore.application.security;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Garde de tenant applicative (Barrière 2). Re-vérifie qu'un identifiant/entité reçu appartient bien
 * au tenant courant avant usage — on ne fait jamais confiance à un ID venu du payload client.
 * La Barrière 3 (RLS) reste le filet de sécurité ultime côté base.
 */
@Service
public class TenantGuard {

    /** Renvoie l'entité si son tenant correspond au tenant courant, sinon échoue en 403. */
    public <T> Mono<T> memeTenant(T entity, UUID tenantDeLEntite) {
        return BusinessContextHolder.currentTenantId()
                .flatMap(courant -> courant.isPresent() && courant.get().equals(tenantDeLEntite)
                        ? Mono.just(entity)
                        : Mono.error(ProblemException.forbidden("Ressource appartenant à un autre tenant")));
    }
}
