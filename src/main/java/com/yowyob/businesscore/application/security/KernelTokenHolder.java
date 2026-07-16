package com.yowyob.businesscore.application.security;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * Porte le JWT <b>délégué</b> de l'utilisateur courant (le token kernel reçu sur la requête) dans le
 * Reactor Context, pour qu'il soit re-transmis tel quel au kernel par {@code KernelClient}.
 *
 * <p>Même principe que {@code BusinessContextHolder} : aucun ThreadLocal, tout passe par le Context
 * Reactor propagé le long de la chaîne réactive. Le filtre de sécurité écrit le token
 * ({@link #withToken}) ; {@code KernelClient} le lit ({@link #currentToken}). Absent en flux machine
 * (client-credentials) ou sur les routes publiques.
 */
public final class KernelTokenHolder {

    private static final String KEY = KernelTokenHolder.class.getName();

    private KernelTokenHolder() {
    }

    /** Écrit le token délégué dans le Reactor Context (utilisé par le filtre de sécurité). */
    public static Context withToken(Context context, String accessToken) {
        return (accessToken == null || accessToken.isBlank()) ? context : context.put(KEY, accessToken);
    }

    /** Lit le token délégué courant s'il existe (sans erreur). */
    public static Mono<Optional<String>> currentToken() {
        return Mono.deferContextual(ctx ->
                Mono.just(ctx.hasKey(KEY) ? Optional.of(ctx.get(KEY)) : Optional.empty()));
    }
}
