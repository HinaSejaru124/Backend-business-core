package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogWriter;
import com.yowyob.businesscore.application.context.BusinessContext;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Journalise les requêtes <b>design-time</b> (JWT du développeur/titulaire — console, ou une
 * application tierce qui rejoue ce JWT) dans {@code requete_log}, catégorie {@code BUSINESS_CORE},
 * {@code facturable = false}.
 *
 * <p>Avant ce filtre, seules les requêtes authentifiées par clé API ({@link UsageTrackingWebFilter})
 * étaient journalisées : toute action de modélisation (déclarer un type métier, une offre, une règle…)
 * restait invisible dans l'onglet Audit / Requêtes du développeur, même si elle avait réellement eu
 * lieu — cf. le cas PharmaCore où la création d'un médicament (design-time, {@code AdminMedicamentController})
 * n'apparaissait jamais. Le développeur voit désormais TOUT ce que son application a fait ; seule la
 * colonne {@code facturable} distingue ce qui compte dans son quota (clé API + Kernel) de ce qui n'y
 * compte jamais (modélisation JWT), conformément à DOCUMENTATION-REQUETES.md.
 *
 * <p>Se positionne comme {@link UsageTrackingWebFilter} mais filtre sur le cas complémentaire (principal
 * {@link BusinessContext} dont l'authentification n'est PAS {@link ApiKeyAuthenticationToken}), pour ne
 * jamais journaliser deux fois la même requête.
 */
public class DesignTimeAuditWebFilter implements WebFilter {

    private static final String ATTRIBUT_DEJA_JOURNALISE = DesignTimeAuditWebFilter.class.getName() + ".journalise";
    private static final String CATEGORIE_BUSINESS_CORE = "BUSINESS_CORE";

    private final RequeteLogWriter requeteLogWriter;

    public DesignTimeAuditWebFilter(RequeteLogWriter requeteLogWriter) {
        this.requeteLogWriter = requeteLogWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long debut = System.currentTimeMillis();
        // chain.filter(exchange) est souscrit EXACTEMENT une fois : le pipeline en aval (handler +
        // écriture de la réponse) ne doit jamais être ré-exécuté. La décision de journaliser (lecture du
        // SecurityContext, toujours disponible dans le Reactor Context à ce stade) est prise APRÈS la
        // réponse. L'ancienne forme `flatMap(...).switchIfEmpty(chain.filter)` ré-abonnait chain.filter :
        // le chemin nominal se terminait « vide » (un Mono<Void> est toujours vide au sens de
        // switchIfEmpty), donc switchIfEmpty relançait TOUTE la suite -> double écriture de la réponse
        // -> UnsupportedOperationException sur en-têtes read-only (setContentLength) + fuite de connexion
        // R2DBC (pool épuisé après quelques requêtes = gel du serveur).
        return chain.filter(exchange)
                .then(Mono.defer(() -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> securityContext.getAuthentication())
                        .filter(auth -> auth != null && auth.isAuthenticated()
                                && !(auth instanceof ApiKeyAuthenticationToken)
                                && auth.getPrincipal() instanceof BusinessContext)
                        .map(auth -> (BusinessContext) auth.getPrincipal())
                        .flatMap(ctx -> journaliser(ctx, exchange, debut))
                        .then()));
    }

    private Mono<Void> journaliser(BusinessContext ctx, ServerWebExchange exchange, long debut) {
        if (exchange.getAttributes().putIfAbsent(ATTRIBUT_DEJA_JOURNALISE, Boolean.TRUE) != null) {
            return Mono.empty();
        }
        HttpStatusCode statut = exchange.getResponse().getStatusCode();
        requeteLogWriter.enregistrerAsync(
                ctx.tenantId(), CATEGORIE_BUSINESS_CORE,
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getPath().value(),
                statut != null ? statut.value() : 0,
                System.currentTimeMillis() - debut,
                false);
        return Mono.empty();
    }
}
