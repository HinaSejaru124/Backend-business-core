package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Comptabilise l'usage par clé API : à chaque requête authentifiée par une clé (X-BC-*), incrémente les
 * compteurs Redis ({@link ApiKeyUsageCompteur}) une fois la réponse produite, en distinguant les erreurs
 * (statut >= 400). Sans effet pour le flux JWT (pas de clé API) ni sur les routes publiques.
 */
public class UsageTrackingWebFilter implements WebFilter {

    private final ApiKeyUsageCompteur compteur;

    public UsageTrackingWebFilter(ApiKeyUsageCompteur compteur) {
        this.compteur = compteur;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof ApiKeyAuthenticationToken token && token.getApiKeyId() != null)
                .map(auth -> ((ApiKeyAuthenticationToken) auth).getApiKeyId())
                .flatMap(apiKeyId -> chain.filter(exchange)
                        .then(Mono.defer(() -> enregistrer(apiKeyId, exchange))))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> enregistrer(UUID apiKeyId, ServerWebExchange exchange) {
        HttpStatusCode statut = exchange.getResponse().getStatusCode();
        boolean erreur = statut != null && statut.isError();
        return compteur.enregistrer(apiKeyId, erreur);
    }
}
