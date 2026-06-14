package com.yowyob.businesscore.adapter.out.kernel.auth;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/** Cache des JWT kernel sur Redis, avec TTL = durée de validité du token. */
@Component
public class RedisJwtCache implements JwtCache {

    private static final String PREFIX = "bc:kerneljwt:";

    private final ReactiveStringRedisTemplate redis;

    public RedisJwtCache(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<String> get(String key) {
        return redis.opsForValue().get(PREFIX + key);
    }

    @Override
    public Mono<Void> put(String key, String token, Duration ttl) {
        return redis.opsForValue().set(PREFIX + key, token, ttl).then();
    }
}
