package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
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
 * Agrège les données du tableau de bord développeur : usage (30 jours) et clés API. L'historique
 * (jours passés) vient de la base ({@code api_key_usage_daily}) ; le jour courant vient des compteurs
 * Redis (source live), ce qui évite tout double comptage avec le flush.
 */
@Service
public class DashboardService {

    private static final int FENETRE_JOURS = 30;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageDailyRepository usageRepository;
    private final ApiKeyUsageCompteur compteur;

    public DashboardService(ApiKeyRepository apiKeyRepository,
                            ApiKeyUsageDailyRepository usageRepository,
                            ApiKeyUsageCompteur compteur) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.compteur = compteur;
    }

    public record PointJour(LocalDate jour, long total) {
    }

    public record DashboardData(
            long requetesCeMois,
            long requetesAujourdhui,
            long erreursAujourdhui,
            double tauxErreur,
            List<PointJour> sparkline,
            List<ApiKeyEntity> cles) {
    }

    public Mono<DashboardData> pour(UUID developerId) {
        return apiKeyRepository.findByDeveloperId(developerId).collectList()
                .flatMap(cles -> {
                    List<UUID> ids = cles.stream().map(ApiKeyEntity::getId).toList();
                    if (ids.isEmpty()) {
                        return Mono.just(new DashboardData(0, 0, 0, 0.0, List.of(), cles));
                    }
                    LocalDate today = LocalDate.now();
                    LocalDate depuis = today.minusDays(FENETRE_JOURS - 1L);

                    Mono<Map<LocalDate, Long>> histo = usageRepository
                            .findByApiKeyIdInAndJourGreaterThanEqual(ids, depuis)
                            .filter(r -> r.getJour().isBefore(today))
                            .collect(HashMap::new, (m, r) ->
                                    m.merge(r.getJour(), r.getTotal(), Long::sum));

                    Mono<long[]> live = Flux.fromIterable(ids)
                            .flatMap(id -> compteur.lireJour(id, today))
                            .reduce(new long[]{0L, 0L}, (acc, u) -> {
                                acc[0] += u.total();
                                acc[1] += u.errors();
                                return acc;
                            });

                    return Mono.zip(histo, live)
                            .map(t -> assembler(cles, today, depuis, t.getT1(), t.getT2()));
                });
    }

    private DashboardData assembler(List<ApiKeyEntity> cles, LocalDate today, LocalDate depuis,
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

        return new DashboardData(ceMois, totalAujourdhui, erreursAujourdhui, tauxErreur, sparkline, cles);
    }
}
