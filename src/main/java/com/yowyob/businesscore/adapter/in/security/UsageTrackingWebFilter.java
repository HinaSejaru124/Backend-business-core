package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.application.billing.QuotaService;
import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Comptabilise l'usage par clé API : à chaque requête authentifiée par une clé (X-BC-*), incrémente une
 * fois la réponse produite (a) les compteurs Redis par clé/jour ({@link ApiKeyUsageCompteur}, en
 * distinguant les erreurs statut &gt;= 400) pour le dashboard, (b) le compteur mensuel par développeur
 * ({@link QuotaService}) pour l'enforcement des quotas, et (c) le journal détaillé
 * {@code requete_log} (catégorie {@code BUSINESS_CORE}, cf. {@link RequeteLogWriter}) pour l'onglet
 * Audit / Requêtes. Sans effet pour le flux JWT ni les routes publiques. Les requêtes refusées par la
 * porte de quota (402) n'atteignent pas ce filtre.
 */
public class UsageTrackingWebFilter implements WebFilter {

    /** Attribut d'exchange marquant qu'une requête a déjà été comptée (garde d'idempotence, voir enregistrer). */
    private static final String ATTRIBUT_DEJA_COMPTE = UsageTrackingWebFilter.class.getName() + ".compte";
    private static final String CATEGORIE_BUSINESS_CORE = "BUSINESS_CORE";

    private final ApiKeyUsageCompteur compteur;
    private final QuotaService quotaService;
    private final RequeteLogWriter requeteLogWriter;

    public UsageTrackingWebFilter(ApiKeyUsageCompteur compteur, QuotaService quotaService,
                                  RequeteLogWriter requeteLogWriter) {
        this.compteur = compteur;
        this.quotaService = quotaService;
        this.requeteLogWriter = requeteLogWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // La télémétrie (requêtes APP) est déjà journalisée par TelemetryController lui-même,
        // avec facturable=false — elle ne doit jamais être recomptée ici (quota / compteurs Redis).
        if (exchange.getRequest().getPath().value().startsWith("/v1/telemetry")) {
            return chain.filter(exchange);
        }
        long debut = System.currentTimeMillis();
        // chain.filter(exchange) souscrit EXACTEMENT une fois (sinon double écriture de la réponse +
        // fuite de connexion R2DBC ; voir DesignTimeAuditWebFilter pour le détail complet du bug). Le
        // comptage d'usage (lecture du SecurityContext, disponible dans le Reactor Context) est fait
        // APRÈS la réponse, sans switchIfEmpty sur un Mono<Void> (toujours « vide »).
        return chain.filter(exchange)
                .then(Mono.defer(() -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> securityContext.getAuthentication())
                        .filter(auth -> auth instanceof ApiKeyAuthenticationToken token && token.getApiKeyId() != null)
                        .map(auth -> (ApiKeyAuthenticationToken) auth)
                        .flatMap(token -> enregistrer(token, exchange, debut))
                        .then()));
    }

    /**
     * Garde d'idempotence : le signal de complétion du filtre chain (chain.filter(exchange).then(...))
     * peut être livré plusieurs fois pour le même exchange (observé empiriquement — la requête et
     * l'authentification, elles, n'ont lieu qu'une fois ; seul ce point de complétion se répète).
     * On ne compte donc qu'une fois par exchange, quel que soit le nombre d'appels à cette méthode.
     */
    private Mono<Void> enregistrer(ApiKeyAuthenticationToken token, ServerWebExchange exchange, long debut) {
        // L'ingestion de télémétrie (requêtes propres de l'app) ne doit ni être comptée dans le quota,
        // ni générer une ligne facturable : reporter sa propre télémétrie est gratuit (cf. TelemetryController).
        if (exchange.getRequest().getPath().value().startsWith("/v1/telemetry")) {
            return Mono.empty();
        }
        if (exchange.getAttributes().putIfAbsent(ATTRIBUT_DEJA_COMPTE, Boolean.TRUE) != null) {
            return Mono.empty();
        }
        HttpStatusCode statut = exchange.getResponse().getStatusCode();
        boolean erreur = statut != null && statut.isError();
        Mono<Void> parCle = compteur.enregistrer(token.getApiKeyId(), erreur);
        Mono<Void> mensuel = token.getDeveloperId() != null
                ? quotaService.compter(token.getDeveloperId())
                : Mono.empty();

        Object principal = token.getPrincipal();
        if (principal instanceof BusinessContext ctx) {
            requeteLogWriter.enregistrerAsync(
                    ctx.tenantId(), CATEGORIE_BUSINESS_CORE,
                    exchange.getRequest().getMethod().name(),
                    exchange.getRequest().getPath().value(),
                    statut != null ? statut.value() : 0,
                    System.currentTimeMillis() - debut,
                    true);
        }

        return parCle.then(mensuel);
    }
}
