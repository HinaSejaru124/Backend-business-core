package com.yowyob.businesscore.adapter.out.cache;

import com.yowyob.businesscore.domain.port.out.VerrouDIdempotence;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Implémentation socle du verrou d'idempotence sur Redis.
 * {@code SET key value NX EX ttl} : la première exécution acquiert le verrou, les rejeux échouent.
 */
@Component
public class RedisVerrouDIdempotence implements VerrouDIdempotence {

    private static final String PREFIX = "bc:idem:";

    private final ReactiveStringRedisTemplate redis;

    public RedisVerrouDIdempotence(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Boolean> acquerir(String cleIdempotence, Duration ttl) {
        return redis.opsForValue().setIfAbsent(PREFIX + cleIdempotence, "1", ttl);
    }

    @Override
    public Mono<Void> liberer(String cleIdempotence) {
        return redis.delete(PREFIX + cleIdempotence).then();
    }
}
