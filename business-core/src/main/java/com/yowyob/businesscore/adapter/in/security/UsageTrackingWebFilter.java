package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.application.billing.QuotaService;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Comptabilise l'usage par clé API : à chaque requête authentifiée par une clé (X-BC-*), incrémente une
 * fois la réponse produite (a) les compteurs Redis par clé/jour ({@link ApiKeyUsageCompteur}, en
 * distinguant les erreurs statut &gt;= 400) pour le dashboard, et (b) le compteur mensuel par développeur
 * ({@link QuotaService}) pour l'enforcement des quotas. Sans effet pour le flux JWT ni les routes
 * publiques. Les requêtes refusées par la porte de quota (402) n'atteignent pas ce filtre.
 */
public class UsageTrackingWebFilter implements WebFilter {

    /** Attribut d'exchange marquant qu'une requête a déjà été comptée (garde d'idempotence, voir enregistrer). */
    private static final String ATTRIBUT_DEJA_COMPTE = UsageTrackingWebFilter.class.getName() + ".compte";

    private final ApiKeyUsageCompteur compteur;
    private final QuotaService quotaService;

    public UsageTrackingWebFilter(ApiKeyUsageCompteur compteur, QuotaService quotaService) {
        this.compteur = compteur;
        this.quotaService = quotaService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(auth -> auth instanceof ApiKeyAuthenticationToken token && token.getApiKeyId() != null)
                .map(auth -> (ApiKeyAuthenticationToken) auth)
                .flatMap(token -> chain.filter(exchange)
                        .then(Mono.defer(() -> enregistrer(token, exchange))))
                // Mono.defer obligatoire ici : voir BusinessContextWebFilter pour l'explication complète
                // (switchIfEmpty évalue son argument avec empressement — sans defer, chain.filter(exchange)
                // s'exécute en trop à chaque requête).
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    /**
     * Garde d'idempotence : le signal de complétion du filtre chain (chain.filter(exchange).then(...))
     * peut être livré plusieurs fois pour le même exchange (observé empiriquement — la requête et
     * l'authentification, elles, n'ont lieu qu'une fois ; seul ce point de complétion se répète).
     * On ne compte donc qu'une fois par exchange, quel que soit le nombre d'appels à cette méthode.
     */
    private Mono<Void> enregistrer(ApiKeyAuthenticationToken token, ServerWebExchange exchange) {
        if (exchange.getAttributes().putIfAbsent(ATTRIBUT_DEJA_COMPTE, Boolean.TRUE) != null) {
            return Mono.empty();
        }
        HttpStatusCode statut = exchange.getResponse().getStatusCode();
        boolean erreur = statut != null && statut.isError();
        Mono<Void> parCle = compteur.enregistrer(token.getApiKeyId(), erreur);
        Mono<Void> mensuel = token.getDeveloperId() != null
                ? quotaService.compter(token.getDeveloperId())
                : Mono.empty();
        return parCle.then(mensuel);
    }
}
