package com.yowyob.businesscore.adapter.in.rest.telemetry;

import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Ingestion de télémétrie : le backend de l'application développeur (ex. PharmaCore) rapporte ici ses
 * <b>propres</b> requêtes (catégorie {@code APP}) — celles que Business Core ne peut pas observer
 * lui-même. Elles ne sont <b>jamais facturables</b> (elles ne consomment que le backend du développeur).
 *
 * <p>Authentifié par clé API (identité runtime de l'application) : le tenant vient de la clé. L'endpoint
 * lui-même est exclu du comptage d'usage (cf. {@code UsageTrackingWebFilter}) — reporter sa télémétrie
 * ne doit pas gonfler la facture. C'est un <b>ajout</b> : aucune route existante n'est modifiée.
 */
@Tag(name = "Télémétrie", description = "Ingestion des requêtes propres de l'application développeur")
@SecurityRequirement(name = "apiKey")
@RestController
@RequestMapping("/v1/telemetry")
public class TelemetryController {

    private static final String CATEGORIE_APP = "APP";

    private final RequeteLogWriter requeteLogWriter;

    public TelemetryController(RequeteLogWriter requeteLogWriter) {
        this.requeteLogWriter = requeteLogWriter;
    }

    /** Une requête propre rapportée par l'application. Champs minimaux, aucune donnée sensible. */
    public record RequeteRapportee(String methode, String endpoint, Integer statutHttp, Long dureeMs) {
    }

    public record LotRequetes(@NotEmpty List<@Valid RequeteRapportee> requetes) {
    }

    @Operation(summary = "Rapporter les requêtes propres de l'application",
            description = "Enregistre un lot de requêtes du backend développeur (catégorie APP, non facturables).")
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingerer(@Valid @RequestBody LotRequetes lot) {
        return BusinessContextHolder.currentTenantId()
                .flatMap(opt -> opt.map(Mono::just).orElseGet(Mono::empty))
                .doOnNext(tenant -> {
                    for (RequeteRapportee r : lot.requetes()) {
                        if (r == null || r.methode() == null || r.endpoint() == null) {
                            continue;
                        }
                        requeteLogWriter.enregistrerAsync(
                                tenant, CATEGORIE_APP,
                                r.methode(), r.endpoint(),
                                r.statutHttp() != null ? r.statutHttp() : 0,
                                r.dureeMs() != null ? r.dureeMs() : 0L,
                                false);
                    }
                })
                .then();
    }
}
