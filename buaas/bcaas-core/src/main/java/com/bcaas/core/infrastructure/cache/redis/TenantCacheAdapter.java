package com.bcaas.core.infrastructure.cache.redis;

import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;

/**
 * Cache Redis pour les Tenants.
 *
 * Les tenants sont fréquemment lus (chaque requête doit résoudre le tenant).
 * Le cache Redis évite des allers-retours en base de données.
 * Analogie réseau : cache ARP — résolution rapide d'adresse sans requête réseau.
 *
 * TTL configurable via bcaas.tenant.cache-ttl-minutes (défaut: 30 min).
 */
@Component
public class TenantCacheAdapter {

    private static final String KEY_PREFIX = "bcaas:tenant:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public TenantCacheAdapter(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${bcaas.tenant.cache-ttl-minutes:30}") int ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public Mono<Void> put(Tenant tenant) {
        String key = KEY_PREFIX + tenant.getId().toString();
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(tenant.getId().toString()))
                .flatMap(value -> redisTemplate.opsForValue().set(key, value, ttl))
                .then();
    }

    public Mono<String> get(TenantId tenantId) {
        String key = KEY_PREFIX + tenantId.toString();
        return redisTemplate.opsForValue().get(key);
    }

    public Mono<Void> evict(TenantId tenantId) {
        String key = KEY_PREFIX + tenantId.toString();
        return redisTemplate.delete(key).then();
    }

    public Mono<Boolean> exists(TenantId tenantId) {
        String key = KEY_PREFIX + tenantId.toString();
        return redisTemplate.hasKey(key);
    }
}
