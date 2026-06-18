package com.yowyob.businesscore.shared.context;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * ⚠️ STUB DE SOCLE — présent uniquement pour le mode standalone.
 * Si le vrai socle est disponible, SUPPRIME ce fichier et importe le BusinessContextHolder du socle.
 *
 * Récupère le BusinessContext depuis le Reactor Context (posé par le BusinessContextWebFilter).
 * Les use cases lisent le tenant ICI, jamais dans le payload.
 */
public final class BusinessContextHolder {

    public static final String KEY = "businessContext";

    private BusinessContextHolder() {
    }

    public static Mono<BusinessContext> current() {
        return Mono.deferContextual(ctx -> ctx.hasKey(KEY)
                ? Mono.just(ctx.get(KEY))
                : Mono.error(new IllegalStateException("BusinessContext absent du contexte réactif")));
    }

    public static Context with(Context ctx, BusinessContext bc) {
        return ctx.put(KEY, bc);
    }
}
