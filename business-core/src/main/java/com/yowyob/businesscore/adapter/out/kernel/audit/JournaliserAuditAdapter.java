package com.yowyob.businesscore.adapter.out.kernel.audit;

import com.yowyob.businesscore.adapter.out.kernel.KernelClient;
import com.yowyob.businesscore.domain.port.out.JournaliserAudit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Implémentation socle de {@link JournaliserAudit} : écrit une trace via le kernel (POST /api/audit),
 * authentifiée au nom du tenant courant par le {@link KernelClient}.
 */
@Component
public class JournaliserAuditAdapter implements JournaliserAudit {

    private final KernelClient kernelClient;

    public JournaliserAuditAdapter(KernelClient kernelClient) {
        this.kernelClient = kernelClient;
    }

    @Override
    public Mono<Void> journaliser(String action, String detail) {
        Map<String, Object> body = Map.of(
                "action", action,
                "detail", detail == null ? "" : detail,
                "timestamp", Instant.now().toString());
        return kernelClient.post("/api/audit", body, Void.class).then();
    }
}
