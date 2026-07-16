package com.yowyob.businesscore.adapter.in.rest.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Santé")
@RestController
public class HealthController {

    @Operation(
            summary = "Sonde de disponibilité",
            description = "Vérifie que le service Business Core répond. Route publique, sans authentification.",
            security = {})
    @ApiResponse(responseCode = "200", description = "Service opérationnel")
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "status", "UP",
                "service", "business-core",
                "timestamp", Instant.now().toString()
        ));
    }
}
