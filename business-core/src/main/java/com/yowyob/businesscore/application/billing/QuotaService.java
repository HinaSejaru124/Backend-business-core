package com.yowyob.businesscore.application.billing;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.cache.QuotaMensuelCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Logique de quota mensuel par développeur : lecture de l'usage (compteur Redis, reconstruit depuis la
 * base si absent), décision de blocage, et incrément après une requête servie.
 *
 * <p>Le blocage ne concerne QUE le trafic authentifié par clé API (machine à machine) ; les plans
 * illimités ({@code quotaMensuel < 0}) ne sont jamais bloqués. L'usage est calculé sur le mois
 * calendaire courant, cohérent avec le {@code requetesCeMois} du tableau de bord.
 */
@Service
public class QuotaService {

    private final QuotaMensuelCompteur compteur;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageDailyRepository usageRepository;
    private final ApiKeyUsageCompteur liveCompteur;
    private final PlanCatalogue catalogue;

    public QuotaService(QuotaMensuelCompteur compteur,
                        ApiKeyRepository apiKeyRepository,
                        ApiKeyUsageDailyRepository usageRepository,
                        ApiKeyUsageCompteur liveCompteur,
                        PlanCatalogue catalogue) {
        this.compteur = compteur;
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.liveCompteur = liveCompteur;
        this.catalogue = catalogue;
    }

    /** État de quota consolidé pour un développeur/plan (utilisé par le tableau de bord). */
    public record EtatQuota(String plan, long quota, long utilise, long restant,
                            boolean illimite, boolean bloque) {
    }

    /** Usage mensuel courant (compteur Redis) ; reconstruit depuis la base au premier accès du mois. */
    public Mono<Long> usageMensuel(UUID developerId) {
        YearMonth mois = YearMonth.now();
        return compteur.lire(developerId, mois)
                .switchIfEmpty(Mono.defer(() -> reconstruire(developerId, mois)
                        .flatMap(base -> compteur.initialiser(developerId, mois, base).thenReturn(base))));
    }

    /** Vrai si la prochaine requête doit être refusée (quota atteint). Toujours faux si illimité. */
    public Mono<Boolean> doitBloquer(UUID developerId, String plan) {
        if (catalogue.illimite(plan)) {
            // On seed quand même le compteur (cohérence si le plan redevient limité plus tard).
            return usageMensuel(developerId).thenReturn(Boolean.FALSE);
        }
        long quota = catalogue.quotaMensuel(plan);
        return usageMensuel(developerId).map(utilise -> utilise >= quota);
    }

    /** État de quota pour affichage (plan, quota, utilisé, restant, illimité, bloqué). */
    public Mono<EtatQuota> etat(UUID developerId, String plan) {
        String planNorm = catalogue.normaliser(plan);
        long quota = catalogue.quotaMensuel(planNorm);
        boolean illimite = catalogue.illimite(planNorm);
        return usageMensuel(developerId).map(utilise -> {
            long restant = illimite ? -1 : Math.max(0, quota - utilise);
            boolean bloque = !illimite && utilise >= quota;
            return new EtatQuota(planNorm, illimite ? -1 : quota, utilise, restant, illimite, bloque);
        });
    }

    /** Incrémente le compteur mensuel après une requête servie (appelé par le filtre d'usage). */
    public Mono<Void> compter(UUID developerId) {
        return compteur.incrementer(developerId, YearMonth.now()).then();
    }

    /**
     * Reconstruit l'usage du mois depuis la base : agrégats quotidiens (jours passés déjà flushés) +
     * compteurs live du jour courant (Redis). Même source que le {@code requetesCeMois} du dashboard.
     */
    private Mono<Long> reconstruire(UUID developerId, YearMonth mois) {
        LocalDate premierJour = mois.atDay(1);
        LocalDate today = LocalDate.now();
        return apiKeyRepository.findByDeveloperId(developerId)
                .map(ApiKeyEntity::getId)
                .collectList()
                .flatMap(ids -> {
                    if (ids.isEmpty()) {
                        return Mono.just(0L);
                    }
                    Mono<Long> histo = usageRepository
                            .findByApiKeyIdInAndJourGreaterThanEqual(ids, premierJour)
                            .filter(r -> r.getJour().isBefore(today))
                            .map(ApiKeyUsageDailyEntity::getTotal)
                            .reduce(0L, Long::sum);
                    Mono<Long> live = Flux.fromIterable(ids)
                            .flatMap(id -> liveCompteur.lireJour(id, today))
                            .map(ApiKeyUsageCompteur.UsageJournalier::total)
                            .reduce(0L, Long::sum);
                    return Mono.zip(histo, live).map(t -> t.getT1() + t.getT2());
                });
    }
}
