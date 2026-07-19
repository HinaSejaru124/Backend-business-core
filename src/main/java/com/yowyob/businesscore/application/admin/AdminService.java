package com.yowyob.businesscore.application.admin;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseContratEntity;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseContratRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseEntity;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogEntity;
import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogRepository;
import com.yowyob.businesscore.application.billing.BillingProperties.PlanDef;
import com.yowyob.businesscore.application.billing.PlanCatalogue;
import com.yowyob.businesscore.application.billing.PlanPricingStore;
import com.yowyob.businesscore.application.billing.QuotaService;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Console d'administration de la plateforme (lecture + actions), réservée aux administrateurs
 * ({@link AdminAccess}). N'expose aucun secret. Agrège des données <b>réelles</b> uniquement.
 *
 * <p><b>RLS</b> : {@code developer_account}, {@code api_key}, {@code api_key_usage_daily} n'ont pas de
 * cloisonnement — l'admin les lit directement. {@code entreprise} et {@code requete_log} sont cloisonnés
 * par tenant : l'admin connaît le {@code kernel_tenant_id} de chaque développeur, donc il exécute ces
 * lectures « dans le contexte » du tenant ciblé ({@link #dansTenant}) — la même machinerie que les
 * requêtes du développeur lui-même, sans superutilisateur ni contournement du schéma.
 */
@Service
public class AdminService {

    private static final int FENETRE_STATS_JOURS = 30;

    private final DeveloperAccountRepository developerRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final EntrepriseContratRepository entrepriseContratRepository;
    private final RequeteLogRepository requeteLogRepository;
    private final QuotaService quotaService;
    private final PlanCatalogue catalogue;
    private final PlanPricingStore pricingStore;

    public AdminService(DeveloperAccountRepository developerRepository,
                        ApiKeyRepository apiKeyRepository,
                        EntrepriseRepository entrepriseRepository,
                        EntrepriseContratRepository entrepriseContratRepository,
                        RequeteLogRepository requeteLogRepository,
                        QuotaService quotaService,
                        PlanCatalogue catalogue,
                        PlanPricingStore pricingStore) {
        this.developerRepository = developerRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.entrepriseContratRepository = entrepriseContratRepository;
        this.requeteLogRepository = requeteLogRepository;
        this.quotaService = quotaService;
        this.catalogue = catalogue;
        this.pricingStore = pricingStore;
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public record Overview(long nbDeveloppeurs, long nbDeveloppeursBloques,
                           long nbClesActives, long nbClesRevoquees,
                           long nbEntreprises, long requetesBusinessCore, long requetesKernelCore,
                           long nbErreursMois,
                           List<PlanCount> repartitionPlans) {
    }

    public record PlanCount(String plan, long nombre) {
    }

    public record DeveloperRow(UUID id, String email, String plan, String status, Instant createdAt,
                               long nbApplications, long nbClesActives,
                               long consoMois, long quota, boolean illimite, double pctConso,
                               long nbErreursMois, Double tempsReponseMoyenMs) {
    }

    public record ApplicationDuDeveloperRow(UUID id, String nom, int numeroVersion, String cycleVie, String callbackUrl) {
    }

    public record ApplicationRow(UUID id, String nom, int numeroVersion, String cycleVie, String callbackUrl,
                                 UUID developerId, String developerEmail) {
    }

    public record CleRow(UUID id, String nom, String status, UUID applicationId,
                         Instant createdAt, Instant lastUsedAt) {
    }

    public record DeveloperDetail(DeveloperRow resume, List<ApplicationDuDeveloperRow> applications, List<CleRow> cles) {
    }

    public record RequeteRow(UUID id, String categorie, String methode, String endpoint,
                             int statutHttp, long dureeMs, boolean facturable, Instant creeLe) {
    }

    public record RequetePage(List<RequeteRow> items, long total, int page, int taille) {
    }

    public record PlanLigne(String code, long prixMensuel, String devise, long quotaMensuel,
                            boolean illimite, long nbAbonnes, long caTheoriqueMensuel) {
    }

    public record BillingSummary(List<PlanLigne> plans, long caTheoriqueMensuelTotal,
                                 long encaisseReel, String devise) {
    }

    // ─── Overview ──────────────────────────────────────────────────────────────

    public Mono<Overview> overview() {
        Mono<List<DeveloperAccountEntity>> devs = developerRepository.findAll().collectList();
        Mono<List<ApiKeyEntity>> cles = apiKeyRepository.findAll().collectList();

        return Mono.zip(devs, cles).flatMap(t -> {
            List<DeveloperAccountEntity> developpeurs = t.getT1();
            List<ApiKeyEntity> toutesCles = t.getT2();

            long actives = toutesCles.stream().filter(c -> ApiKeyEntity.STATUT_ACTIVE.equals(c.getStatus())).count();
            long revoquees = toutesCles.stream().filter(c -> ApiKeyEntity.STATUT_REVOKED.equals(c.getStatus())).count();
            long bloques = developpeurs.stream().filter(d -> !"ACTIVE".equalsIgnoreCase(d.getStatus())).count();

            Map<String, Long> parPlan = new LinkedHashMap<>();
            for (DeveloperAccountEntity d : developpeurs) {
                parPlan.merge(catalogue.normaliser(d.getPlan()), 1L, Long::sum);
            }
            List<PlanCount> repartition = parPlan.entrySet().stream()
                    .map(e -> new PlanCount(e.getKey(), e.getValue())).toList();

            Instant depuisErreurs = Instant.now().minus(FENETRE_STATS_JOURS, java.time.temporal.ChronoUnit.DAYS);

            // Agrégats cloisonnés (entreprises + requêtes par catégorie + erreurs) : sommés par tenant réel.
            Flux<long[]> parDev = Flux.fromIterable(developpeurs)
                    .filter(d -> d.getKernelTenantId() != null)
                    .flatMap(d -> {
                        UUID tenant = d.getKernelTenantId();
                        Mono<Long> nbEnt = dansTenant(tenant, entrepriseRepository.countByTenantId(tenant))
                                .defaultIfEmpty(0L);
                        Mono<Long> nbBc = dansTenant(tenant,
                                requeteLogRepository.countByTenantIdAndCategorie(tenant, "BUSINESS_CORE"))
                                .defaultIfEmpty(0L);
                        Mono<Long> nbKnl = dansTenant(tenant,
                                requeteLogRepository.countByTenantIdAndCategorie(tenant, "KNL_CORE"))
                                .defaultIfEmpty(0L);
                        Mono<Long> nbErreurs = dansTenant(tenant,
                                requeteLogRepository.statsParTenant(tenant, depuisErreurs)
                                        .map(RequeteLogRepository.StatsRow::nbErreurs))
                                .defaultIfEmpty(0L);
                        return Mono.zip(nbEnt, nbBc, nbKnl, nbErreurs)
                                .map(z -> new long[]{z.getT1(), z.getT2(), z.getT3(),
                                        z.getT4() != null ? z.getT4() : 0L});
                    });

            return parDev.reduce(new long[]{0L, 0L, 0L, 0L}, (acc, v) -> {
                acc[0] += v[0];
                acc[1] += v[1];
                acc[2] += v[2];
                acc[3] += v[3];
                return acc;
            }).map(sum -> new Overview(
                    developpeurs.size(), bloques, actives, revoquees,
                    sum[0], sum[1], sum[2], sum[3], repartition));
        });
    }

    // ─── Liste des développeurs ────────────────────────────────────────────────

    public Flux<DeveloperRow> developpeurs() {
        return developerRepository.findAll()
                .flatMap(this::ligneDeveloppeur)
                .sort(Comparator.comparing(DeveloperRow::createdAt));
    }

    private Mono<DeveloperRow> ligneDeveloppeur(DeveloperAccountEntity dev) {
        UUID tenant = dev.getKernelTenantId();
        Mono<Long> nbEnt = tenant == null ? Mono.just(0L)
                : dansTenant(tenant, entrepriseRepository.countByTenantId(tenant)).defaultIfEmpty(0L);
        Mono<Long> nbClesActives = apiKeyRepository
                .countByDeveloperIdAndStatus(dev.getId(), ApiKeyEntity.STATUT_ACTIVE).defaultIfEmpty(0L);
        Mono<QuotaService.EtatQuota> quota = quotaService.etat(dev.getId(), dev.getPlan());
        Mono<RequeteLogRepository.StatsRow> stats = tenant == null
                ? Mono.just(new RequeteLogRepository.StatsRow(0L, null))
                : dansTenant(tenant, requeteLogRepository.statsParTenant(
                        tenant, Instant.now().minus(FENETRE_STATS_JOURS, java.time.temporal.ChronoUnit.DAYS)))
                        .defaultIfEmpty(new RequeteLogRepository.StatsRow(0L, null));

        return Mono.zip(nbEnt, nbClesActives, quota, stats).map(t -> {
            QuotaService.EtatQuota q = t.getT3();
            RequeteLogRepository.StatsRow s = t.getT4();
            double pct = q.illimite() || q.quota() <= 0 ? 0.0
                    : Math.min(100.0, (double) q.utilise() / (double) q.quota() * 100.0);
            return new DeveloperRow(
                    dev.getId(), dev.getEmail(), catalogue.normaliser(dev.getPlan()), dev.getStatus(),
                    dev.getCreatedAt(), t.getT1(), t.getT2(),
                    q.utilise(), q.illimite() ? -1 : q.quota(), q.illimite(), pct,
                    s.nbErreurs() != null ? s.nbErreurs() : 0L, s.dureeMoyenneMs());
        });
    }

    // ─── Vue globale des applications de la plateforme ──────────────────────────

    /** Toutes les applications (entreprises) enregistrées sur la plateforme, tous développeurs confondus. */
    public Flux<ApplicationRow> applications() {
        return developerRepository.findAll()
                .filter(dev -> dev.getKernelTenantId() != null)
                .flatMap(dev -> {
                    UUID tenant = dev.getKernelTenantId();
                    return dansTenant(tenant, entrepriseRepository.findAll()
                            .flatMap(e -> entrepriseContratRepository.findById(e.getId())
                                    .map(EntrepriseContratEntity::getCallbackUrl)
                                    .defaultIfEmpty("")
                                    .map(callbackUrl -> new ApplicationRow(e.getId(), e.getNom(),
                                            e.getNumeroVersion(), e.getCycleVie(),
                                            callbackUrl.isEmpty() ? null : callbackUrl,
                                            dev.getId(), dev.getEmail())))
                            .collectList())
                            .flatMapMany(Flux::fromIterable);
                });
    }

    // ─── Détail d'un développeur ────────────────────────────────────────────────

    public Mono<DeveloperDetail> detail(UUID developerId) {
        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Développeur introuvable : " + developerId)))
                .flatMap(dev -> {
                    UUID tenant = dev.getKernelTenantId();
                    Mono<List<ApplicationDuDeveloperRow>> applications = tenant == null ? Mono.just(List.of())
                            : dansTenant(tenant, entrepriseRepository.findAll()
                                    .flatMap(e -> entrepriseContratRepository.findById(e.getId())
                                            .map(EntrepriseContratEntity::getCallbackUrl)
                                            .defaultIfEmpty("")
                                            .map(callbackUrl -> new ApplicationDuDeveloperRow(e.getId(), e.getNom(),
                                                    e.getNumeroVersion(), e.getCycleVie(),
                                                    callbackUrl.isEmpty() ? null : callbackUrl)))
                                    .collectList());
                    Mono<List<CleRow>> cles = apiKeyRepository.findByDeveloperId(developerId)
                            .map(c -> new CleRow(c.getId(), c.getName(), c.getStatus(), c.getEntrepriseId(),
                                    c.getCreatedAt(), c.getLastUsedAt()))
                            .collectList();
                    return Mono.zip(ligneDeveloppeur(dev), applications, cles)
                            .map(t -> new DeveloperDetail(t.getT1(), t.getT2(), t.getT3()));
                });
    }

    // ─── Track : historique des requêtes FACTURABLES d'un développeur ───────────
    // L'admin ne voit QUE les requêtes facturables (KNL_CORE + BUSINESS_CORE) — jamais les requêtes
    // propres (APP) du backend développeur. Mêmes filtres serveur que la console développeur.

    public Mono<RequetePage> track(UUID developerId, String categorie, String methode, String periode,
                                   String statut, int page, int taille) {
        int tailleEff = Math.min(Math.max(1, taille), 100);
        int pageEff = Math.max(0, page);
        long decalage = (long) pageEff * tailleEff;

        String cat = vide(categorie) ? null : categorie.trim().toUpperCase(Locale.ROOT);
        // Garde-fou : l'admin ne peut pas demander la catégorie APP (non facturable, invisible pour lui).
        if ("APP".equals(cat)) {
            cat = null;
        }
        String meth = vide(methode) ? null : methode.trim().toUpperCase(Locale.ROOT);
        Instant depuis = bornePeriode(periode);
        Integer erreurFlag = flagStatut(statut);

        final String catF = cat;
        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Développeur introuvable : " + developerId)))
                .flatMap(dev -> {
                    UUID tenant = dev.getKernelTenantId();
                    if (tenant == null) {
                        return Mono.just(new RequetePage(List.of(), 0, pageEff, tailleEff));
                    }
                    Mono<List<RequeteRow>> items = requeteLogRepository
                            .pageFiltree(tenant, catF, meth, depuis, erreurFlag, 1, tailleEff, decalage)
                            .map(e -> new RequeteRow(e.getId(), e.getCategorie(), e.getMethode(), e.getEndpoint(),
                                    e.getStatutHttp(), e.getDureeMs() != null ? e.getDureeMs() : 0L,
                                    e.getFacturable() != null ? e.getFacturable() : true, e.getCreeLe()))
                            .collectList();
                    Mono<Long> total = requeteLogRepository
                            .countFiltree(tenant, catF, meth, depuis, erreurFlag, 1)
                            .defaultIfEmpty(0L);

                    return dansTenant(tenant, Mono.zip(items, total)
                            .map(t -> new RequetePage(t.getT1(), t.getT2(), pageEff, tailleEff)));
                });
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static Instant bornePeriode(String periode) {
        if (vide(periode)) {
            return null;
        }
        return switch (periode.trim().toUpperCase(Locale.ROOT)) {
            case "JOUR" -> Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS);
            case "SEMAINE" -> Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
            case "MOIS" -> Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
            default -> null;
        };
    }

    private static Integer flagStatut(String statut) {
        if (vide(statut)) {
            return null;
        }
        return switch (statut.trim().toUpperCase(Locale.ROOT)) {
            case "ERREUR" -> 1;
            case "OK" -> 0;
            default -> null;
        };
    }

    // ─── Facturation (comptabilité) ─────────────────────────────────────────────

    public Mono<BillingSummary> billing() {
        return developerRepository.findAll().collectList().map(devs -> {
            Map<String, Long> abonnes = new LinkedHashMap<>();
            for (DeveloperAccountEntity d : devs) {
                abonnes.merge(catalogue.normaliser(d.getPlan()), 1L, Long::sum);
            }

            List<PlanLigne> lignes = new ArrayList<>();
            long total = 0L;
            String devise = "XAF";
            for (Map.Entry<String, PlanDef> e : catalogue.plans().entrySet()) {
                String code = e.getKey();
                PlanDef def = e.getValue();
                long nb = abonnes.getOrDefault(code, 0L);
                long ca = def.prixMensuel() * nb;
                total += ca;
                devise = def.devise();
                lignes.add(new PlanLigne(code, def.prixMensuel(), def.devise(),
                        def.quotaMensuel(), def.illimite(), nb, ca));
            }
            // encaisseReel = 0 : le paiement réel passe par Kernel Core (indisponible). On ne fabrique
            // jamais un revenu encaissé — seul le facturé théorique est calculé (cf. AUDIT-PHARMACORE.md).
            return new BillingSummary(lignes, total, 0L, devise);
        });
    }

    // ─── Tarification éditable par l'admin ──────────────────────────────────────

    public record PricingRow(String code, long quotaMensuel, long prixMensuel, String devise, boolean illimite) {
    }

    /** Tarification courante (mémoire = source de vérité vivante), triée FREE → PRO → ENTERPRISE → autres. */
    public List<PricingRow> pricing() {
        return pricingStore.snapshot().entrySet().stream()
                .map(e -> new PricingRow(e.getKey(), e.getValue().quotaMensuel(),
                        e.getValue().prixMensuel(), e.getValue().devise(), e.getValue().illimite()))
                .sorted(Comparator.comparingInt(r -> ordrePlan(r.code())))
                .toList();
    }

    /** Fixe la tarification d'un plan (prix, quota, devise) — persistée, effet immédiat sur le calcul. */
    public Mono<Void> definirTarif(String code, long quotaMensuel, long prixMensuel, String devise) {
        if (code == null || code.isBlank()) {
            return Mono.error(ProblemException.badRequest("Code de plan requis."));
        }
        if (prixMensuel < 0) {
            return Mono.error(ProblemException.badRequest("Le prix ne peut pas être négatif."));
        }
        return pricingStore.definir(code, quotaMensuel, prixMensuel, devise);
    }

    private static int ordrePlan(String code) {
        return switch (code) {
            case "FREE" -> 0;
            case "PRO" -> 1;
            case "ENTERPRISE" -> 2;
            default -> 9;
        };
    }

    // ─── Actions ────────────────────────────────────────────────────────────────

    public Mono<Void> bloquerDeveloppeur(UUID developerId) {
        return changerStatutDeveloppeur(developerId, "SUSPENDED");
    }

    public Mono<Void> debloquerDeveloppeur(UUID developerId) {
        return changerStatutDeveloppeur(developerId, "ACTIVE");
    }

    private Mono<Void> changerStatutDeveloppeur(UUID developerId, String statut) {
        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Développeur introuvable : " + developerId)))
                .flatMap(dev -> {
                    dev.setStatus(statut);
                    return developerRepository.save(dev);
                })
                .then();
    }

    /** Révoque une clé API (par son id), quel que soit le développeur. Immédiat et définitif. */
    public Mono<Void> revoquerCle(UUID cleId) {
        return apiKeyRepository.findById(cleId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Clé introuvable : " + cleId)))
                .flatMap(cle -> {
                    cle.setStatus(ApiKeyEntity.STATUT_REVOKED);
                    return apiKeyRepository.save(cle);
                })
                .then();
    }

    // ─── Helper RLS : exécuter une lecture « dans le contexte » d'un tenant ──────

    private <T> Mono<T> dansTenant(UUID tenantId, Mono<T> source) {
        BusinessContext ctx = new BusinessContext(tenantId, null, null, null, null, null);
        return source.contextWrite(c -> BusinessContextHolder.withContext(c, ctx));
    }
}
