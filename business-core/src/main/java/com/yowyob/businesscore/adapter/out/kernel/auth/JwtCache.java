package com.yowyob.businesscore.adapter.out.kernel.auth;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cache des JWT kernel courts (évite de refaire /oauth2/token à chaque appel).
 * Impl par défaut sur Redis ; les tests peuvent fournir une implémentation en mémoire.
 */
public interface JwtCache {

    Mono<String> get(String key);

    Mono<Void> put(String key, String token, Duration ttl);
}
