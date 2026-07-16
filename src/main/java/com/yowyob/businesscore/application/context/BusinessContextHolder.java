package com.yowyob.businesscore.application.context;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès au {@link BusinessContext} dans le contexte réactif (Reactor Context).
 *
 * <p>Le filtre de sécurité écrit le contexte ({@link #withContext}) ; les use cases et la couche
 * de persistance le lisent ({@link #currentContext} / {@link #currentTenantId}). Aucun ThreadLocal :
 * tout passe par le Context Reactor, propagé le long de la chaîne réactive.
 */
public final class BusinessContextHolder {

    /** Clé de stockage du contexte dans le Reactor Context. */
    public static final Class<BusinessContext> CONTEXT_KEY = BusinessContext.class;

    private BusinessContextHolder() {
    }

    /** Écrit le contexte dans le Reactor Context (utilisé par le filtre de sécurité). */
    public static Context withContext(Context context, BusinessContext businessContext) {
        return context.put(CONTEXT_KEY, businessContext);
    }

    /**
     * Lit le contexte courant, ou {@link Mono#empty()} s'il est absent.
     *
     * <p>Volontairement non bloquant/non erroné : sur les routes authentifiées, le filtre de sécurité
     * garantit la présence du contexte, donc l'absence ne survient en pratique que lors de la
     * re-souscription interne de WebFlux à la finalisation de la réponse (drain de
     * {@code ChannelSendOperator}), avec un Reactor Context vide. Renvoyer {@code empty} y court-circuite
     * proprement le {@code flatMap} des controllers (aucune ré-exécution, aucune erreur parasite loguée).
     * L'autorisation reste <b>fail-closed</b> : {@code AuthorizationService} refuse explicitement sur absence.
     */
    public static Mono<BusinessContext> currentContext() {
        return Mono.deferContextual(ctx ->
                ctx.hasKey(CONTEXT_KEY)
                        ? Mono.just(ctx.get(CONTEXT_KEY))
                        : Mono.empty());
    }

    /** Lit le tenant courant s'il existe, sans erreur (utilisé par la couche de persistance/RLS). */
    public static Mono<Optional<UUID>> currentTenantId() {
        return Mono.deferContextual(ctx ->
                Mono.just(ctx.hasKey(CONTEXT_KEY)
                        ? Optional.of(((BusinessContext) ctx.get(CONTEXT_KEY)).tenantId())
                        : Optional.empty()));
    }
}
