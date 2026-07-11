package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.adapter.out.persistence.trace.TraceOperationRepository;
import com.yowyob.businesscore.adapter.out.persistence.trace.TraceOperationRepository.ActiviteRow;
import com.yowyob.businesscore.adapter.out.persistence.trace.TraceOperationRepository.TopEntrepriseRow;
import com.yowyob.businesscore.adapter.out.persistence.trace.TraceOperationRepository.TopOperationRow;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agrège les données du tableau de bord développeur : usage global (30 jours, tous business confondus),
 * compteurs publics (entreprises, clés actives) et activité métier (top opérations/entreprises, activité
 * récente — depuis {@code trace_operation}). Aucune donnée secrète n'est exposée ici — pour la gestion
 * d'une clé précise (créer/renommer/révoquer), voir {@code /v1/businesses/{id}/api-keys}.
 *
 * <p>L'historique (jours passés) vient de la base ({@code api_key_usage_daily}) ; le jour courant vient
 * des compteurs Redis (source live), ce qui évite tout double comptage avec le flush.
 */
@Service
public class DashboardService {

    private static final int FENETRE_JOURS = 30;
    private static final int TOP_LIMITE = 5;
    private static final int ACTIVITE_LIMITE = 10;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyUsageDailyRepository usageRepository;
    private final ApiKeyUsageCompteur compteur;
    private final EntrepriseRepository entrepriseRepository;
    private final TraceOperationRepository traceRepository;

    public DashboardService(ApiKeyRepository apiKeyRepository,
                            ApiKeyUsageDailyRepository usageRepository,
                            ApiKeyUsageCompteur compteur,
                            EntrepriseRepository entrepriseRepository,
                            TraceOperationRepository traceRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.compteur = compteur;
        this.entrepriseRepository = entrepriseRepository;
        this.traceRepository = traceRepository;
    }

    public record PointJour(LocalDate jour, long total) {
    }

    public record TopOperation(String nom, long total) {
    }

    public record TopEntreprise(UUID entrepriseId, String nom, long total) {
    }

    public record ActiviteItem(UUID entrepriseId, String entrepriseNom, String operationNom, String statut,
                               Instant creeLe) {
    }

    public record DashboardData(
            long requetesCeMois,
            long requetesAujourdhui,
            long erreursAujourdhui,
            double tauxErreur,
            List<PointJour> sparkline,
            long nombreEntreprises,
            long nombreClesActives,
            List<TopOperation> topOperations,
            List<TopEntreprise> topEntreprises,
            List<ActiviteItem> activiteRecente) {
    }

    public Mono<DashboardData> pour(UUID developerId, UUID tenantId) {
        Instant depuisTrace = LocalDate.now().minusDays(FENETRE_JOURS - 1L).atStartOfDay(ZoneOffset.UTC).toInstant();

        Mono<List<TopOperation>> topOperations = traceRepository
                .topOperations(tenantId, depuisTrace, TOP_LIMITE)
                .map(r -> new TopOperation(r.operationNom(), r.total()))
                .collectList();
        Mono<List<TopEntreprise>> topEntreprises = traceRepository
                .topEntreprises(tenantId, depuisTrace, TOP_LIMITE)
                .map(r -> new TopEntreprise(r.entrepriseId(), r.nom(), r.total()))
                .collectList();
        Mono<List<ActiviteItem>> activiteRecente = traceRepository
                .activiteRecente(tenantId, ACTIVITE_LIMITE)
                .map(r -> new ActiviteItem(r.entrepriseId(), r.entrepriseNom(), r.operationNom(), r.statut(), r.creeLe()))
                .collectList();

        return Mono.zip(
                entrepriseRepository.countByTenantId(tenantId).defaultIfEmpty(0L),
                apiKeyRepository.countByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                        .defaultIfEmpty(0L),
                apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                        .map(ApiKeyEntity::getId).collectList(),
                topOperations, topEntreprises, activiteRecente
        ).flatMap(t -> usageEtAssemblage(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6()));
    }

    private Mono<DashboardData> usageEtAssemblage(long nombreEntreprises, long nombreClesActives, List<UUID> ids,
                                                  List<TopOperation> topOperations, List<TopEntreprise> topEntreprises,
                                                  List<ActiviteItem> activiteRecente) {
        if (ids.isEmpty()) {
            return Mono.just(new DashboardData(0, 0, 0, 0.0, List.of(), nombreEntreprises, nombreClesActives,
                    topOperations, topEntreprises, activiteRecente));
        }
        LocalDate today = LocalDate.now();
        LocalDate depuis = today.minusDays(FENETRE_JOURS - 1L);

        Mono<Map<LocalDate, Long>> histo = usageRepository
                .findByApiKeyIdInAndJourGreaterThanEqual(ids, depuis)
                .filter(r -> r.getJour().isBefore(today))
                .collect(HashMap::new, (m, r) ->
                        m.merge(r.getJour(), r.getTotal(), (a, b) -> Long.sum(a, b)));

        // L'usage live (Redis) est un bonus d'affichage, pas une dépendance dure : une panne Redis ne
        // doit jamais casser tout le dashboard (même filet de sécurité que ApiKeyUsageCompteur.enregistrer).
        Mono<long[]> live = Flux.fromIterable(ids)
                .flatMap(id -> compteur.lireJour(id, today).onErrorResume(ex -> Mono.empty()))
                .reduce(new long[]{0L, 0L}, (acc, u) -> {
                    acc[0] += u.total();
                    acc[1] += u.errors();
                    return acc;
                });

        return Mono.zip(histo, live)
                .map(t -> assembler(nombreEntreprises, nombreClesActives, today, depuis, t.getT1(), t.getT2(),
                        topOperations, topEntreprises, activiteRecente));
    }

    private DashboardData assembler(long nombreEntreprises, long nombreClesActives, LocalDate today,
                                    LocalDate depuis, Map<LocalDate, Long> histo, long[] live,
                                    List<TopOperation> topOperations, List<TopEntreprise> topEntreprises,
                                    List<ActiviteItem> activiteRecente) {
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

        return new DashboardData(ceMois, totalAujourdhui, erreursAujourdhui, tauxErreur, sparkline,
                nombreEntreprises, nombreClesActives, topOperations, topEntreprises, activiteRecente);
    }
}
