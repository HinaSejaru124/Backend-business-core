package com.yowyob.businesscore.application.security;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Point d'autorisation réactif du socle (S.2). Authentifié != autorisé : avant une action sensible,
 * on vérifie que l'acteur courant porte le rôle métier requis (rôles du BusinessContext).
 * Les briques Règles/Opérations s'en servent (dont l'effet DEROGER limité à certains rôles).
 */
@Service
public class AuthorizationService {

    /** Complète si le rôle est présent, sinon échoue en 403 RFC 7807. */
    public Mono<Void> exigerRole(String role) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> ctx.hasRole(role)
                        ? Mono.empty()
                        : Mono.error(ProblemException.forbidden("Rôle métier requis : " + role)));
    }

    /** Vrai si l'acteur courant porte au moins un des rôles donnés. */
    public Mono<Boolean> aLUnDesRoles(String... roles) {
        return BusinessContextHolder.currentContext()
                .map(ctx -> {
                    for (String role : roles) {
                        if (ctx.hasRole(role)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}
