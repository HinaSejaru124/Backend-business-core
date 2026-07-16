package com.yowyob.businesscore.adapter.out.persistence.requestlog;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.UUID;

/**
 * Écrit une ligne du journal détaillé des requêtes ({@code requete_log}) — catégorie
 * {@code KNL_CORE} (Business Core → Kernel) ou {@code BUSINESS_CORE} (clé API → Business Core).
 *
 * <p>Insert « fire-and-forget » : un incident d'écriture du journal ne doit <b>jamais</b> faire
 * échouer la requête réelle qu'il journalise (même garde-fou que {@code ApiKeyUsageCompteur}).
 * Souscrit sur un scheduler séparé pour ne pas retarder la libération de la connexion HTTP.
 *
 * <p><b>Contexte réactif reconstruit explicitement</b> : cette souscription est détachée de la chaîne
 * d'origine (fire-and-forget), donc le Reactor Context de la requête (qui porte le {@link BusinessContext})
 * ne se propage pas automatiquement. Or {@code TenantConnectionPoolFactory} lit ce contexte pour poser
 * {@code app.current_tenant} sur la connexion — sans lui, la policy RLS rejetterait l'insertion (silencieuse
 * sinon). On réinjecte donc un {@link BusinessContext} minimal (seul {@code tenantId} est utilisé ici).
 */
@Component
public class RequeteLogWriter {

    private static final Logger log = LoggerFactory.getLogger(RequeteLogWriter.class);

    private final RequeteLogRepository repository;

    public RequeteLogWriter(RequeteLogRepository repository) {
        this.repository = repository;
    }

    public void enregistrerAsync(UUID tenantId, String categorie, String methode, String endpoint,
                                 int statutHttp, long dureeMs, boolean facturable) {
        if (tenantId == null) {
            return;
        }
        RequeteLogEntity entity = RequeteLogEntity.nouvelle(
                tenantId, categorie, methode, tronquer(endpoint), statutHttp, dureeMs, facturable, Instant.now());
        BusinessContext ctxMinimal = new BusinessContext(tenantId, null, null, null, null, null);
        repository.save(entity)
                .contextWrite(ctx -> BusinessContextHolder.withContext(ctx, ctxMinimal))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.warn("Échec écriture requete_log ({} {}) : {}", methode, endpoint, ex.toString());
                    return Mono.empty();
                })
                .subscribe();
    }

    private static String tronquer(String endpoint) {
        if (endpoint == null) {
            return "?";
        }
        return endpoint.length() > 500 ? endpoint.substring(0, 500) : endpoint;
    }
}
