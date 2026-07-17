package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.in.rest.error.ProblemResponseWriter;
import com.yowyob.businesscore.application.billing.QuotaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Porte de quota mensuel. Pour le trafic authentifié par clé API (machine à machine), refuse la requête
 * avec <b>HTTP 402 Payment Required</b> quand le développeur a atteint le quota mensuel de son plan.
 *
 * <p>Sans effet sur le flux JWT (console développeur : le dev doit toujours pouvoir consulter son
 * tableau de bord et changer de plan même bloqué) ni sur les plans illimités. Placée AVANT le filtre de
 * comptage d'usage : une requête bloquée n'est pas comptabilisée (elle n'atteint jamais le service).
 */
public class QuotaEnforcementWebFilter implements WebFilter {

    private final QuotaService quotaService;
    private final ProblemResponseWriter writer;

    public QuotaEnforcementWebFilter(QuotaService quotaService, ProblemResponseWriter writer) {
        this.quotaService = quotaService;
        this.writer = writer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // La télémétrie (requêtes APP, jamais facturables) ne doit jamais être bloquée par le quota :
        // un développeur bloqué doit quand même pouvoir rapporter ce que fait son backend.
        if (exchange.getRequest().getPath().value().startsWith("/v1/telemetry")) {
            return chain.filter(exchange);
        }
        // chain.filter(exchange) souscrit EXACTEMENT une fois (voir BusinessContextWebFilter pour le
        // détail du bug de double abonnement via switchIfEmpty sur un Mono<Void>). On résout d'abord la
        // décision de quota en Optional<String> (présent = plan dont le quota est atteint → refus 402 ;
        // vide = ne pas bloquer : trafic non-clé-API, ou clé API sous le quota), toujours exactement un
        // élément grâce à defaultIfEmpty. Un unique flatMap tranche ensuite : refuser (la requête
        // n'atteint jamais le handler) OU forwarder une seule fois. L'ancienne forme laissait passer la
        // requête APRÈS avoir écrit le 402 (switchIfEmpty relançait chain.filter) — corrigé ici.
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(auth -> auth instanceof ApiKeyAuthenticationToken token
                        && token.getApiKeyId() != null && token.getDeveloperId() != null)
                .map(auth -> (ApiKeyAuthenticationToken) auth)
                .flatMap(token -> quotaService.doitBloquer(token.getDeveloperId(), token.getPlan())
                        .map(bloque -> Boolean.TRUE.equals(bloque)
                                ? Optional.of(token.getPlan())
                                : Optional.<String>empty()))
                .defaultIfEmpty(Optional.empty())
                .flatMap(planBloque -> planBloque
                        .map(plan -> refuser(exchange, plan))
                        .orElseGet(() -> chain.filter(exchange)));
    }

    private Mono<Void> refuser(ServerWebExchange exchange, String plan) {
        String detail = "Quota mensuel du plan " + plan + " atteint. "
                + "Passez à un plan supérieur pour continuer à consommer l'API.";
        return writer.write(exchange, HttpStatus.PAYMENT_REQUIRED, "Quota mensuel dépassé", detail);
    }
}
