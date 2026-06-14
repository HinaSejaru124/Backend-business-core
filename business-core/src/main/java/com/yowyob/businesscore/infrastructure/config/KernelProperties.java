package com.yowyob.businesscore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'accès au kernel RT-Comops. En dev/test, {@code baseUrl} pointe vers un mock
 * (WireMock) ; en prod, vers http://kernel-core.yowyob.com. Changer de cible = changer une propriété.
 */
@ConfigurationProperties(prefix = "businesscore.kernel")
public record KernelProperties(String baseUrl, long timeoutMs, int maxRetries) {

    public KernelProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8089";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 5000;
        }
        if (maxRetries < 0) {
            maxRetries = 0;
        }
    }
}
