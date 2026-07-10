package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrège les données du tableau de bord développeur : plan &amp; quota, usage (30 jours) et clés API.
 * L'historique (jours passés) vient de la base ({@code api_key_usage_daily}) ; le jour courant vient des
 * compteurs Redis (source live), ce qui évite tout double comptage avec le flush. Le quota mensuel et
 * les requêtes restantes sont dérivés du plan du compte et du {@link PlanCatalogue}, cohérents avec
 * {@code requetesCeMois}.
 */
@Service
public class DashboardService {

    private static final int FENETRE_JOURS = 30;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageDailyRepository usageRepository;
    private final ApiKeyUsageCompteur compteur;
    private final DeveloperAccountRepository developerRepository;
    private final PlanCatalogue catalogue;

    public DashboardService(ApiKeyRepository apiKeyRepository,
                            ApiKeyUsageDailyRepository usageRepository,
                            ApiKeyUsageCompteur compteur,
                            DeveloperAccountRepository developerRepository,
                            PlanCatalogue catalogue) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.compteur = compteur;
        this.developerRepository = developerRepository;
        this.catalogue = catalogue;
    }

    public record PointJour(LocalDate jour, long total) {
    }

    public record DashboardData(
            String plan,
            long quotaMensuel,
            long requetesRestantes,
            boolean bloque,
            long requetesCeMois,
            long requetesAujourdhui,
            long erreursAujourdhui,
            double tauxErreur,
            List<PointJour> sparkline,
            List<ApiKeyEntity> cles) {
    }

    public Mono<DashboardData> pour(UUID developerId) {
        return developerRepository.findById(developerId)
                .map(DeveloperAccountEntity::getPlan)
                .defaultIfEmpty(PlanCatalogue.PLAN_DEFAUT)
                .flatMap(plan -> donnees(developerId, plan));
    }

    private Mono<DashboardData> donnees(UUID developerId, String plan) {
        return apiKeyRepository.findByDeveloperId(developerId).collectList()
                .flatMap(cles -> {
                    List<UUID> ids = cles.stream().map(ApiKeyEntity::getId).toList();
                    if (ids.isEmpty()) {
                        return Mono.just(assembler(plan, cles, LocalDate.now(), LocalDate.now(),
                                Map.of(), new long[]{0L, 0L}));
                    }
                    LocalDate today = LocalDate.now();
                    LocalDate depuis = today.minusDays(FENETRE_JOURS - 1L);

                    Mono<Map<LocalDate, Long>> histo = usageRepository
                            .findByApiKeyIdInAndJourGreaterThanEqual(ids, depuis)
                            .filter(r -> r.getJour().isBefore(today))
                            .collect(HashMap::new, (m, r) ->
                                    m.merge(r.getJour(), r.getTotal(), (a, b) -> Long.sum(a, b)));

                    Mono<long[]> live = Flux.fromIterable(ids)
                            .flatMap(id -> compteur.lireJour(id, today))
                            .reduce(new long[]{0L, 0L}, (acc, u) -> {
                                acc[0] += u.total();
                                acc[1] += u.errors();
                                return acc;
                            });

                    return Mono.zip(histo, live)
                            .map(t -> assembler(plan, cles, today, depuis, t.getT1(), t.getT2()));
                });
    }

    private DashboardData assembler(String plan, List<ApiKeyEntity> cles, LocalDate today, LocalDate depuis,
                                    Map<LocalDate, Long> histo, long[] live) {
        long totalAujourdhui = live[0];
        long erreursAujourdhui = live[1];

        List<PointJour> sparkline = new ArrayList<>();
        long ceMois = 0L;
        for (LocalDate jour = depuis; !jour.isAfter(today); jour = jour.plusDays(1)) {
            long total = jour.isEqual(today) ? totalAujourdhui : histo.getOrDefault(jour, 0L);
            sparkline.add(new PointJour(jour, total));
            if (jour.getYear() == today.getYear() && jour.getMonthValue() == today.getMonthValue()) {
                ceMois += total;
            }
        }

        double tauxErreur = totalAujourdhui == 0 ? 0.0
                : (double) erreursAujourdhui / (double) totalAujourdhui;

        String planNorm = catalogue.normaliser(plan);
        boolean illimite = catalogue.illimite(planNorm);
        long quota = catalogue.quotaMensuel(planNorm);
        long quotaAffiche = illimite ? -1 : quota;
        long restant = illimite ? -1 : Math.max(0, quota - ceMois);
        boolean bloque = !illimite && ceMois >= quota;

        return new DashboardData(planNorm, quotaAffiche, restant, bloque, ceMois,
                totalAujourdhui, erreursAujourdhui, tauxErreur, sparkline, cles);
    }
}
