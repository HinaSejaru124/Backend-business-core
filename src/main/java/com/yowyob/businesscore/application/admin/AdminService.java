
package com.yowyob.businesscore.application.admin;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseContratEntity;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseContratRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseEntity;
import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestEntity;
import com.yowyob.businesscore.adapter.out.persistence.billing.PlanChangeRequestRepository;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private final PlanChangeRequestRepository changeRepository;
    private final QuotaService quotaService;
    private final PlanCatalogue catalogue;
    private final PlanPricingStore pricingStore;
    private final AdminProperties adminProperties;

    public AdminService(DeveloperAccountRepository developerRepository,
                        ApiKeyRepository apiKeyRepository,
                        EntrepriseRepository entrepriseRepository,
                        EntrepriseContratRepository entrepriseContratRepository,
                        RequeteLogRepository requeteLogRepository,
                        PlanChangeRequestRepository changeRepository,
                        QuotaService quotaService,
                        PlanCatalogue catalogue,
                        PlanPricingStore pricingStore,
                        AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
        this.developerRepository = developerRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.entrepriseContratRepository = entrepriseContratRepository;
        this.requeteLogRepository = requeteLogRepository;
        this.changeRepository = changeRepository;
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
                               long nbErreursMois, Double tempsReponseMoyenMs, String entrepriseNom) {
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
            // Exclut l'administrateur des statistiques développeurs (il n'est pas un client de la plateforme).
            List<DeveloperAccountEntity> developpeurs = t.getT1().stream()
                    .filter(d -> !adminProperties.estAdmin(d.getEmail()))
                    .toList();
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
        // L'administrateur de la plateforme N'EST PAS un développeur : on l'exclut de la liste et des stats,
        // même s'il possède un developer_account (créé par l'auth kernel).
        return developerRepository.findAll()
                .filter(dev -> !adminProperties.estAdmin(dev.getEmail()))
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
                    s.nbErreurs() != null ? s.nbErreurs() : 0L, s.dureeMoyenneMs(), dev.getEntrepriseNom());
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
                                    .mapNotNull(EntrepriseContratEntity::getCallbackUrl)
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
                                            .mapNotNull(EntrepriseContratEntity::getCallbackUrl)
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
                    // Vue admin plateforme : toutes les applications du développeur (applicationId = null).
                    Mono<List<RequeteRow>> items = requeteLogRepository
                            .pageFiltree(tenant, null, catF, meth, depuis, erreurFlag, 1, tailleEff, decalage)
                            .map(e -> new RequeteRow(e.getId(), e.getCategorie(), e.getMethode(), e.getEndpoint(),
                                    e.getStatutHttp(), e.getDureeMs() != null ? e.getDureeMs() : 0L,
                                    e.getFacturable() != null ? e.getFacturable() : true, e.getCreeLe()))
                            .collectList();
                    Mono<Long> total = requeteLogRepository
                            .countFiltree(tenant, null, catF, meth, depuis, erreurFlag, 1)
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

    // ─── Trafic global (flux temps réel de TOUTES les requêtes de la plateforme) ─

    public record TraficRow(UUID id, String developerEmail, String applicationNom, String categorie,
                            String methode, String endpoint, int statutHttp, long dureeMs,
                            boolean facturable, Instant creeLe) {
    }

    public record TraficPage(List<TraficRow> items, long total, int page, int taille) {
    }

    private static Instant borneTrafic(String periode) {
        if (vide(periode)) {
            return null;
        }
        return switch (periode.trim().toLowerCase(Locale.ROOT)) {
            case "1h" -> Instant.now().minus(1, ChronoUnit.HOURS);
            case "24h" -> Instant.now().minus(24, ChronoUnit.HOURS);
            case "7j" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "30j" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> null;
        };
    }

    /**
     * Flux global de toutes les requêtes de la plateforme (tous développeurs / applications), filtrable.
     * Les deux seules catégories exposées sont « API Business Core » (facturable) et « Application »
     * (non facturable) — le Kernel n'est jamais mentionné. Agrégé par tenant (RLS), fusionné et paginé.
     */
    public Mono<TraficPage> trafic(UUID developerId, UUID applicationId, Boolean facturable, String statut,
                                   String periode, String recherche, int page, int taille) {
        int tailleEff = Math.min(Math.max(1, taille), 100);
        int pageEff = Math.max(0, page);
        Instant depuis = borneTrafic(periode);
        Integer erreurFlag = flagStatut(statut);
        Integer facturableFlag = facturable == null ? null : (facturable ? 1 : 0);
        String terme = vide(recherche) ? null : recherche.trim().toLowerCase(Locale.ROOT);
        // Fenêtre de lignes à charger pour couvrir la page demandée (bornée pour ne pas tout ramener).
        int fenetre = Math.min((pageEff + 1) * tailleEff, 1000);

        return developerRepository.findAll()
                .filter(d -> d.getKernelTenantId() != null)
                .filter(d -> !adminProperties.estAdmin(d.getEmail()))
                .filter(d -> developerId == null || developerId.equals(d.getId()))
                .flatMap(d -> {
                    UUID tenant = d.getKernelTenantId();
                    Mono<Long> count = dansTenant(tenant, requeteLogRepository
                            .countFiltree(tenant, applicationId, null, null, depuis, erreurFlag, facturableFlag))
                            .defaultIfEmpty(0L);
                    Mono<Map<UUID, String>> noms = dansTenant(tenant, entrepriseRepository.findAll().collectList()
                            .map(list -> {
                                Map<UUID, String> m = new HashMap<>();
                                for (EntrepriseEntity e : list) m.put(e.getId(), e.getNom());
                                return m;
                            })).defaultIfEmpty(Map.of());
                    Mono<List<RequeteLogEntity>> rows = dansTenant(tenant, requeteLogRepository
                            .pageFiltree(tenant, applicationId, null, null, depuis, erreurFlag, facturableFlag,
                                    fenetre, 0)
                            .collectList()).defaultIfEmpty(List.of());
                    return Mono.zip(count, noms, rows).map(t -> {
                        Map<UUID, String> m = t.getT2();
                        List<TraficRow> out = new ArrayList<>();
                        for (RequeteLogEntity e : t.getT3()) {
                            boolean fact = e.getFacturable() == null || e.getFacturable();
                            out.add(new TraficRow(e.getId(), d.getEmail(),
                                    e.getEntrepriseId() == null ? null : m.get(e.getEntrepriseId()),
                                    fact ? "API Business Core" : "Application",
                                    e.getMethode(), e.getEndpoint(),
                                    e.getStatutHttp() != null ? e.getStatutHttp() : 0,
                                    e.getDureeMs() != null ? e.getDureeMs() : 0L,
                                    fact, e.getCreeLe()));
                        }
                        return new Object[]{t.getT1(), out};
                    });
                })
                .collectList()
                .map(all -> {
                    List<TraficRow> merged = new ArrayList<>();
                    long totalCompte = 0L;
                    for (Object[] chunk : all) {
                        totalCompte += (long) chunk[0];
                        @SuppressWarnings("unchecked")
                        List<TraficRow> part = (List<TraficRow>) chunk[1];
                        merged.addAll(part);
                    }
                    merged.sort(Comparator.comparing(TraficRow::creeLe, Comparator.nullsLast(Comparator.reverseOrder())));
                    // La recherche par endpoint n'est pas gérée en SQL : on filtre la fenêtre et le total
                    // reflète alors la fenêtre chargée (approché au-delà de 1000 lignes).
                    long total;
                    if (terme != null) {
                        merged.removeIf(r -> r.endpoint() == null || !r.endpoint().toLowerCase(Locale.ROOT).contains(terme));
                        total = merged.size();
                    } else {
                        total = totalCompte;
                    }
                    int from = pageEff * tailleEff;
                    List<TraficRow> items = from >= merged.size() ? List.of()
                            : new ArrayList<>(merged.subList(from, Math.min(from + tailleEff, merged.size())));
                    return new TraficPage(items, total, pageEff, tailleEff);
                });
    }

    // ─── Facturation (comptabilité) ─────────────────────────────────────────────

    public Mono<BillingSummary> billing() {
        return Mono.zip(developerRepository.findAll().collectList(), changeRepository.findAll().collectList())
                .map(t -> {
                    List<DeveloperAccountEntity> devs = t.getT1();
                    List<PlanChangeRequestEntity> changes = t.getT2();

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

                    // Encaissé réel = somme des paiements CONFIRMÉS du mois courant (transactions réelles).
                    Instant debutMois = ZonedDateTime.now(ZoneOffset.UTC).withDayOfMonth(1)
                            .truncatedTo(ChronoUnit.DAYS).toInstant();
                    Map<String, PlanDef> tarifs = pricingStore.snapshot();
                    long encaisse = changes.stream()
                            .filter(c -> "CONFIRME".equals(c.getStatut()))
                            .filter(c -> estPaiementReel(c.getPaymentReference()))
                            .filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(debutMois))
                            .mapToLong(c -> prixDe(tarifs, c.getPlanTo()))
                            .sum();
                    return new BillingSummary(lignes, total, encaisse, devise);
                });
    }

    private static long prixDe(Map<String, PlanDef> tarifs, String code) {
        PlanDef def = tarifs.get(code == null ? "" : code.trim().toUpperCase(Locale.ROOT));
        return def != null ? def.prixMensuel() : 0L;
    }

    /**
     * Un paiement est <b>réel</b> (donc encaissable en comptabilité) seulement s'il porte une référence
     * d'ordre Kernel (MyCoolPay). Garde-fou historique : d'anciennes lignes de test portent une référence
     * {@code SIMULATION-…} (l'adapter de simulation a été retiré) — elles n'ont débité aucun compte mobile
     * money et ne doivent jamais être comptabilisées comme un revenu.
     */
    private static boolean estPaiementReel(String reference) {
        return reference != null && !reference.startsWith("SIMULATION-");
    }

    // ─── Achats de forfaits (transactions réelles) + série de revenus ───────────

    public record TransactionRow(UUID id, String developerEmail, String planFrom, String planTo,
                                 long montant, String devise, String statut, Instant createdAt,
                                 String paymentReference) {
    }

    /** Historique des achats de forfait (plan_change_request), filtrable par statut (EN_ATTENTE/CONFIRME/REFUSE). */
    public Mono<List<TransactionRow>> transactions(String statut) {
        String filtre = vide(statut) ? null : statut.trim().toUpperCase(Locale.ROOT);
        return Mono.zip(developerRepository.findAll().collectList(), changeRepository.findAll().collectList())
                .map(t -> {
                    Map<UUID, String> email = new HashMap<>();
                    for (DeveloperAccountEntity d : t.getT1()) email.put(d.getId(), d.getEmail());
                    Map<String, PlanDef> tarifs = pricingStore.snapshot();
                    return t.getT2().stream()
                            // On n'affiche que de vraies transactions kernel — jamais les artefacts de simulation.
                            .filter(c -> estPaiementReel(c.getPaymentReference()))
                            .filter(c -> filtre == null || filtre.equals(c.getStatut()))
                            .sorted(Comparator.comparing(PlanChangeRequestEntity::getCreatedAt,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                            .map(c -> {
                                PlanDef def = tarifs.get(c.getPlanTo());
                                return new TransactionRow(c.getId(),
                                        email.getOrDefault(c.getDeveloperId(), "développeur inconnu"),
                                        c.getPlanFrom(), c.getPlanTo(),
                                        prixDe(tarifs, c.getPlanTo()),
                                        def != null ? def.devise() : "XAF",
                                        c.getStatut(), c.getCreatedAt(), c.getPaymentReference());
                            })
                            .toList();
                });
    }

    /** Série temporelle des revenus RÉELLEMENT encaissés (paiements CONFIRMÉS), selon la période. */
    public Mono<List<TimeseriesPoint>> revenueTimeseries(String periode) {
        Gran g = granularite(periode);
        List<Instant> attendus = bucketsAttendus(g);
        Instant depuis = attendus.get(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(g.pattern(), Locale.FRENCH).withZone(ZoneOffset.UTC);
        Map<String, PlanDef> tarifs = pricingStore.snapshot();

        return changeRepository.findAll().collectList().map(changes -> {
            Map<Instant, Long> merged = new HashMap<>();
            for (PlanChangeRequestEntity c : changes) {
                if (!"CONFIRME".equals(c.getStatut()) || c.getCreatedAt() == null) continue;
                if (!estPaiementReel(c.getPaymentReference())) continue; // jamais de revenu simulé
                if (c.getCreatedAt().isBefore(depuis)) continue;
                merged.merge(cle(c.getCreatedAt(), g.unite()), prixDe(tarifs, c.getPlanTo()), Long::sum);
            }
            List<TimeseriesPoint> pts = new ArrayList<>();
            for (Instant inst : attendus) {
                long rev = merged.getOrDefault(inst, 0L);
                pts.add(new TimeseriesPoint(fmt.format(inst), rev, rev, 0L));
            }
            return pts;
        });
    }

    // ─── Tarification éditable par l'admin ──────────────────────────────────────

    public record PricingRow(String code, String libelle, long quotaMensuel, long prixMensuel, String devise,
                             boolean illimite, long applicationsMax, boolean applicationsIllimite) {
    }

    /** Tarification courante (mémoire = source de vérité vivante), triée FREE → PRO → ENTERPRISE → autres. */
    public List<PricingRow> pricing() {
        return pricingStore.snapshot().entrySet().stream()
                .map(e -> new PricingRow(e.getKey(), e.getValue().libelle(), e.getValue().quotaMensuel(),
                        e.getValue().prixMensuel(), e.getValue().devise(), e.getValue().illimite(),
                        e.getValue().applicationsMax(), e.getValue().illimiteApplications()))
                .sorted(Comparator.comparingInt(r -> ordrePlan(r.code())))
                .toList();
    }

    /** Fixe la tarification d'un plan (prix, quota, devise, limite d'apps, libellé) — persistée, effet immédiat. */
    public Mono<Void> definirTarif(String code, long quotaMensuel, long prixMensuel, String devise,
                                   long applicationsMax, String libelle) {
        if (code == null || code.isBlank()) {
            return Mono.error(ProblemException.badRequest("Code de plan requis."));
        }
        if (!code.trim().matches("(?i)[A-Z0-9_]{2,32}")) {
            return Mono.error(ProblemException.badRequest(
                    "Code de plan invalide : lettres/chiffres/underscore uniquement (2 à 32 caractères)."));
        }
        if (prixMensuel < 0) {
            return Mono.error(ProblemException.badRequest("Le prix ne peut pas être négatif."));
        }
        return pricingStore.definir(code, quotaMensuel, prixMensuel, devise, applicationsMax, libelle);
    }

    /**
     * Supprime un forfait. Garde-fous : le plan FREE (défaut) est protégé, et un plan encore utilisé par
     * des développeurs ne peut pas être supprimé (il faut d'abord les réaffecter) — jamais de compte orphelin.
     */
    public Mono<Void> supprimerPlan(String code) {
        String norm = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        if (norm.isEmpty()) {
            return Mono.error(ProblemException.badRequest("Code de plan requis."));
        }
        if (PlanCatalogue.PLAN_DEFAUT.equals(norm)) {
            return Mono.error(ProblemException.badRequest("Le forfait " + norm + " est le plan par défaut : il ne peut pas être supprimé."));
        }
        return developerRepository.findAll()
                .filter(d -> norm.equals(catalogue.normaliser(d.getPlan())))
                .count()
                .flatMap(n -> n > 0
                        ? Mono.error(ProblemException.badRequest(
                                "Impossible de supprimer " + norm + " : " + n + " développeur(s) y sont abonnés. Réaffectez-les d'abord."))
                        : pricingStore.supprimer(norm));
    }

    private static int ordrePlan(String code) {
        return switch (code) {
            case "FREE" -> 0;
            case "PRO" -> 1;
            case "ENTERPRISE" -> 2;
            default -> 9;
        };
    }

    // ─── Séries temporelles + top applications + activité (dashboard) ───────────

    public record TimeseriesPoint(String label, long total, long facturables, long erreurs) {
    }

    public record TopApp(UUID id, String nom, String developerEmail, long requetes) {
    }

    public record ActivityItem(String type, String label, String detail, Instant at) {
    }

    /** Granularité d'une série : unité SQL date_trunc, borne de départ, nb de points, format d'étiquette. */
    private record Gran(String unite, int nbPoints, String pattern) {
    }

    private static Gran granularite(String periode) {
        String p = periode == null ? "MOIS" : periode.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "JOUR" -> new Gran("hour", 24, "HH'h'");
            case "SEMAINE" -> new Gran("day", 7, "dd/MM");
            case "ANNEE" -> new Gran("month", 12, "MMM");
            default -> new Gran("day", 30, "dd/MM"); // MOIS
        };
    }

    private static List<Instant> bucketsAttendus(Gran g) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<Instant> out = new ArrayList<>();
        switch (g.unite()) {
            case "hour" -> {
                ZonedDateTime start = now.truncatedTo(ChronoUnit.HOURS).minusHours(g.nbPoints() - 1L);
                for (int i = 0; i < g.nbPoints(); i++) out.add(start.plusHours(i).toInstant());
            }
            case "month" -> {
                ZonedDateTime start = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusMonths(g.nbPoints() - 1L);
                for (int i = 0; i < g.nbPoints(); i++) out.add(start.plusMonths(i).toInstant());
            }
            default -> {
                ZonedDateTime start = now.truncatedTo(ChronoUnit.DAYS).minusDays(g.nbPoints() - 1L);
                for (int i = 0; i < g.nbPoints(); i++) out.add(start.plusDays(i).toInstant());
            }
        }
        return out;
    }

    private static Instant cle(Instant t, String unite) {
        ZonedDateTime z = t.atZone(ZoneOffset.UTC);
        return switch (unite) {
            case "hour" -> z.truncatedTo(ChronoUnit.HOURS).toInstant();
            case "month" -> z.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).toInstant();
            default -> z.truncatedTo(ChronoUnit.DAYS).toInstant();
        };
    }

    /** Série temporelle des requêtes de TOUTE la plateforme (tous tenants fusionnés), gap-comblée. */
    public Mono<List<TimeseriesPoint>> requestsTimeseries(String periode) {
        Gran g = granularite(periode);
        List<Instant> attendus = bucketsAttendus(g);
        Instant depuis = attendus.get(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(g.pattern(), Locale.FRENCH).withZone(ZoneOffset.UTC);

        return developerRepository.findAll()
                .filter(d -> d.getKernelTenantId() != null)
                .flatMap(d -> {
                    UUID tenant = d.getKernelTenantId();
                    return dansTenant(tenant, requeteLogRepository.serieParTenant(tenant, g.unite(), depuis).collectList())
                            .defaultIfEmpty(List.of());
                })
                .collectList()
                .map(all -> {
                    Map<Instant, long[]> merged = new HashMap<>();
                    for (List<RequeteLogRepository.SerieBucket> liste : all) {
                        for (RequeteLogRepository.SerieBucket b : liste) {
                            if (b.bucket() == null) continue;
                            long[] acc = merged.computeIfAbsent(cle(b.bucket(), g.unite()), k -> new long[3]);
                            acc[0] += b.total() != null ? b.total() : 0L;
                            acc[1] += b.facturables() != null ? b.facturables() : 0L;
                            acc[2] += b.erreurs() != null ? b.erreurs() : 0L;
                        }
                    }
                    List<TimeseriesPoint> pts = new ArrayList<>();
                    for (Instant inst : attendus) {
                        long[] v = merged.getOrDefault(inst, new long[3]);
                        pts.add(new TimeseriesPoint(fmt.format(inst), v[0], v[1], v[2]));
                    }
                    return pts;
                });
    }

    /** Top applications par nombre de requêtes réelles (toutes plateformes), sur la fenêtre de la période. */
    public Mono<List<TopApp>> topApplications(String periode, int limite) {
        Gran g = granularite(periode);
        Instant depuis = bucketsAttendus(g).get(0);
        int lim = Math.max(1, Math.min(limite, 50));
        return developerRepository.findAll()
                .filter(d -> d.getKernelTenantId() != null)
                .flatMap(d -> {
                    UUID tenant = d.getKernelTenantId();
                    return dansTenant(tenant, requeteLogRepository.topAppsParTenant(tenant, depuis)
                            .flatMap(ac -> entrepriseRepository.findById(ac.entrepriseId())
                                    .map(e -> new TopApp(ac.entrepriseId(), e.getNom(), d.getEmail(),
                                            ac.total() != null ? ac.total() : 0L))
                                    .defaultIfEmpty(new TopApp(ac.entrepriseId(), "Application",
                                            d.getEmail(), ac.total() != null ? ac.total() : 0L)))
                            .collectList())
                            .flatMapMany(Flux::fromIterable);
                })
                .sort(Comparator.comparingLong(TopApp::requetes).reversed())
                .take(lim)
                .collectList();
    }

    /** Activité récente assemblée d'enregistrements RÉELS : inscriptions de développeurs + clés API. */
    public Mono<List<ActivityItem>> activiteRecente(int limite) {
        int lim = Math.max(1, Math.min(limite, 30));
        Mono<List<DeveloperAccountEntity>> devsM = developerRepository.findAll().collectList();
        Mono<List<ApiKeyEntity>> clesM = apiKeyRepository.findAll().collectList();
        return Mono.zip(devsM, clesM).map(t -> {
            List<DeveloperAccountEntity> devs = t.getT1();
            List<ApiKeyEntity> cles = t.getT2();
            Map<UUID, String> emailParDev = new HashMap<>();
            for (DeveloperAccountEntity d : devs) emailParDev.put(d.getId(), d.getEmail());

            List<ActivityItem> items = new ArrayList<>();
            for (DeveloperAccountEntity d : devs) {
                if (adminProperties.estAdmin(d.getEmail())) continue; // l'admin n'est pas un développeur
                items.add(new ActivityItem("DEVELOPER", "Nouveau développeur inscrit", d.getEmail(), d.getCreatedAt()));
            }
            for (ApiKeyEntity c : cles) {
                boolean revoquee = ApiKeyEntity.STATUT_REVOKED.equals(c.getStatus());
                String qui = emailParDev.getOrDefault(c.getDeveloperId(), "développeur inconnu");
                items.add(new ActivityItem(
                        revoquee ? "KEY_REVOKED" : "KEY_CREATED",
                        revoquee ? "Clé API révoquée" : "Nouvelle clé API",
                        c.getName() + " · " + qui,
                        c.getCreatedAt()));
            }
            items.sort(Comparator.comparing(ActivityItem::at,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return items.size() > lim ? items.subList(0, lim) : items;
        });
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

    /** Force le forfait d'un développeur (effet immédiat sur son quota). Le plan doit exister au catalogue. */
    public Mono<Void> changerPlan(UUID developerId, String plan) {
        String code = plan == null ? "" : plan.trim().toUpperCase(Locale.ROOT);
        if (!pricingStore.snapshot().containsKey(code)) {
            return Mono.error(ProblemException.badRequest("Plan inconnu : " + plan));
        }
        return developerRepository.findById(developerId)
                .switchIfEmpty(Mono.error(ProblemException.notFound("Développeur introuvable : " + developerId)))
                .flatMap(dev -> {
                    dev.setPlan(code);
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
