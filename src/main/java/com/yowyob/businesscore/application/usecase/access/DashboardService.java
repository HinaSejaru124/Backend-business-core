package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.cache.ApiKeyUsageCompteur;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyUsageDailyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogRepository;
import com.yowyob.businesscore.adapter.out.persistence.trace.TraceOperationRepository;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
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
 * Agrège les données du tableau de bord développeur : plan &amp; quota, usage global (30 jours),
 * compteurs publics (entreprises, clés actives) et activité métier (top opérations/entreprises,
 * activité récente — depuis {@code trace_operation}). Aucune donnée secrète n'est exposée ici —
 * pour la gestion d'une clé précise (créer/renommer/révoquer), voir {@code /v1/applications/{id}/api-keys}.
 *
 * <p>L'historique (jours passés) vient de la base ({@code api_key_usage_daily}) ; le jour courant vient
 * des compteurs Redis (source live), ce qui évite tout double comptage avec le flush. Le quota mensuel
 * et les requêtes restantes sont dérivés du plan du compte et du {@link PlanCatalogue}.
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
    private final DeveloperAccountRepository developerRepository;
    private final RequeteLogRepository requeteLogRepository;
    private final PlanCatalogue catalogue;

    public DashboardService(ApiKeyRepository apiKeyRepository,
                            ApiKeyUsageDailyRepository usageRepository,
                            ApiKeyUsageCompteur compteur,
                            EntrepriseRepository entrepriseRepository,
                            TraceOperationRepository traceRepository,
                            DeveloperAccountRepository developerRepository,
                            RequeteLogRepository requeteLogRepository,
                            PlanCatalogue catalogue) {
        this.apiKeyRepository = apiKeyRepository;
        this.usageRepository = usageRepository;
        this.compteur = compteur;
        this.entrepriseRepository = entrepriseRepository;
        this.traceRepository = traceRepository;
        this.developerRepository = developerRepository;
        this.requeteLogRepository = requeteLogRepository;
        this.catalogue = catalogue;
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
            String plan,
            long quotaMensuel,
            long requetesRestantes,
            boolean bloque,
            long requetesCeMois,
            long requetesAujourdhui,
            long erreursAujourdhui,
            double tauxErreur,
            List<PointJour> sparkline,
            long nombreEntreprises,
            long nombreClesActives,
            Double tempsReponseMoyenMs,
            List<TopOperation> topOperations,
            List<TopEntreprise> topEntreprises,
            List<ActiviteItem> activiteRecente) {
    }

    public Mono<DashboardData> pour(UUID developerId, UUID tenantId) {
        return developerRepository.findById(developerId)
                .map(DeveloperAccountEntity::getPlan)
                .defaultIfEmpty(PlanCatalogue.PLAN_DEFAUT)
                .flatMap(plan -> donnees(developerId, tenantId, plan));
    }

    /**
     * Sparkline seule, sur une fenêtre choisie par l'utilisateur (sélecteur de période du graphique).
     * Volontairement <b>découplé</b> de {@link #pour} : le quota mensuel / requêtes restantes / blocage
     * ({@code ceMois}) ne doivent JAMAIS dépendre de la fenêtre d'affichage du graphique — seule cette
     * méthode varie, la facturation reste toujours calculée sur le mois calendaire réel.
     *
     * <p>Granularité réelle : journalière (nos compteurs n'ont pas de grain horaire). Une fenêtre de
     * 1 jour renvoie donc un seul point réel, pas une fausse répartition par heure.
     */
    public Mono<List<PointJour>> sparklinePour(UUID developerId, int fenetreJours) {
        LocalDate today = LocalDate.now();
        LocalDate depuis = today.minusDays(Math.max(1, fenetreJours) - 1L);

        return apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                .map(ApiKeyEntity::getId)
                .collectList()
                .flatMap(ids -> {
                    if (ids.isEmpty()) {
                        return Mono.just(pointsVides(depuis, today));
                    }
                    Mono<Map<LocalDate, Long>> histo = usageRepository
                            .findByApiKeyIdInAndJourGreaterThanEqual(ids, depuis)
                            .filter(r -> r.getJour().isBefore(today))
                            .collect(HashMap::new, (m, r) ->
                                    m.merge(r.getJour(), r.getTotal(), (a, b) -> Long.sum(a, b)));
                    Mono<Long> aujourdhui = Flux.fromIterable(ids)
                            .flatMap(id -> compteur.lireJour(id, today).onErrorResume(ex -> Mono.empty()))
                            .reduce(0L, (acc, u) -> acc + u.total());
                    return Mono.zip(histo, aujourdhui).map(t -> {
                        List<PointJour> points = new ArrayList<>();
                        for (LocalDate jour = depuis; !jour.isAfter(today); jour = jour.plusDays(1)) {
                            long total = jour.isEqual(today) ? t.getT2() : t.getT1().getOrDefault(jour, 0L);
                            points.add(new PointJour(jour, total));
                        }
                        return points;
                    });
                });
    }

    private List<PointJour> pointsVides(LocalDate depuis, LocalDate today) {
        List<PointJour> points = new ArrayList<>();
        for (LocalDate jour = depuis; !jour.isAfter(today); jour = jour.plusDays(1)) {
            points.add(new PointJour(jour, 0L));
        }
        return points;
    }

    private Mono<DashboardData> donnees(UUID developerId, UUID tenantId, String plan) {
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
        // Temps de réponse moyen (bonus d'affichage) : une panne sur cet agrégat ne doit jamais casser
        // le dashboard, cf. même principe que le compteur Redis live plus bas.
        Mono<Double> tempsReponseMoyenMs = requeteLogRepository.statsParTenant(tenantId, depuisTrace)
                .map(RequeteLogRepository.StatsRow::dureeMoyenneMs)
                .onErrorResume(ex -> Mono.empty())
                .defaultIfEmpty(null);

        return Mono.zip(
                entrepriseRepository.countByTenantId(tenantId).defaultIfEmpty(0L),
                apiKeyRepository.countByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                        .defaultIfEmpty(0L),
                apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE)
                        .map(ApiKeyEntity::getId).collectList(),
                topOperations, topEntreprises, activiteRecente
        ).flatMap(t -> tempsReponseMoyenMs.flatMap(temps ->
                usageEtAssemblage(plan, t.getT1(), t.getT2(), t.getT3(), temps, t.getT4(), t.getT5(), t.getT6())));
    }

    private Mono<DashboardData> usageEtAssemblage(String plan,
                                                  long nombreEntreprises, long nombreClesActives, List<UUID> ids,
                                                  Double tempsReponseMoyenMs,
                                                  List<TopOperation> topOperations, List<TopEntreprise> topEntreprises,
                                                  List<ActiviteItem> activiteRecente) {
        LocalDate today = LocalDate.now();
        LocalDate depuis = today.minusDays(FENETRE_JOURS - 1L);

        if (ids.isEmpty()) {
            return Mono.just(assembler(plan, nombreEntreprises, nombreClesActives, tempsReponseMoyenMs,
                    today, depuis, Map.of(), new long[]{0L, 0L},
                    topOperations, topEntreprises, activiteRecente));
        }

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
                .map(t -> assembler(plan, nombreEntreprises, nombreClesActives, tempsReponseMoyenMs,
                        today, depuis, t.getT1(), t.getT2(),
                        topOperations, topEntreprises, activiteRecente));
    }

    private DashboardData assembler(String plan,
                                    long nombreEntreprises, long nombreClesActives, Double tempsReponseMoyenMs,
                                    LocalDate today, LocalDate depuis,
                                    Map<LocalDate, Long> histo, long[] live,
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

        String planNorm = catalogue.normaliser(plan);
        boolean illimite = catalogue.illimite(planNorm);
        long quota = catalogue.quotaMensuel(planNorm);
        long quotaAffiche = illimite ? -1 : quota;
        long restant = illimite ? -1 : Math.max(0, quota - ceMois);
        boolean bloque = !illimite && ceMois >= quota;

        return new DashboardData(
                planNorm, quotaAffiche, restant, bloque,
                ceMois, totalAujourdhui, erreursAujourdhui, tauxErreur, sparkline,
                nombreEntreprises, nombreClesActives, tempsReponseMoyenMs,
                topOperations, topEntreprises, activiteRecente);
    }
}
