package com.yowyob.businesscore._standalone;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.businesscore.shared.kernel.KernelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ⚠️ STANDALONE ONLY — fausse implémentation du KernelClient pour faire tourner l'appli sans kernel.
 * Renvoie des réponses synthétiques (id aléatoire, soldes à 0). À SUPPRIMER avec le socle réel,
 * qui fournit le vrai KernelClient (auth X-Client-Id / X-Api-Key / Bearer automatique).
 */
@Component
public class KernelClientStub implements KernelClient {

    private static final Logger log = LoggerFactory.getLogger(KernelClientStub.class);
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T> Mono<T> post(String path, Object body, Class<T> type) {
        log.info("[KERNEL-STUB] POST {} body={}", path, body);
        return Mono.fromSupplier(() -> fake(type));
    }

    @Override
    public <T> Mono<T> get(String path, Class<T> type) {
        log.info("[KERNEL-STUB] GET {}", path);
        return Mono.fromSupplier(() -> fake(type));
    }

    @Override
    public <T> Mono<T> postForOrganization(UUID organizationId, String path, Object body, Class<T> type) {
        log.info("[KERNEL-STUB] POST(org={}) {} body={}", organizationId, path, body);
        return Mono.fromSupplier(() -> fake(type));
    }

    @Override
    public <T> Mono<T> getForOrganization(UUID organizationId, String path, Class<T> type) {
        log.info("[KERNEL-STUB] GET(org={}) {}", organizationId, path);
        return Mono.fromSupplier(() -> fake(type));
    }

    private <T> T fake(Class<T> type) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", UUID.randomUUID());
        m.put("amount", BigDecimal.ZERO);
        m.put("balance", BigDecimal.ZERO);
        m.put("ok", true);
        return mapper.convertValue(m, type);
    }
}
