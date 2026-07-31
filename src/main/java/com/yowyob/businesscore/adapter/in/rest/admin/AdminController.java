package com.yowyob.businesscore.adapter.in.rest.admin;

import com.yowyob.businesscore.application.admin.AdminAccess;
import com.yowyob.businesscore.application.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Console d'administration de la plateforme Business Core (réservée aux administrateurs).
 *
 * <p>Chaque route est gardée par {@link AdminAccess#exigerAdmin()} : un développeur non-admin reçoit
 * {@code 403}. Toutes les données sont réelles (aucune fabrication). Ce contrôleur est un <b>ajout</b> :
 * il ne modifie aucune route existante et n'expose aucun secret.
 */
@Tag(name = "Admin", description = "Console d'administration de la plateforme")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final AdminAccess adminAccess;
    private final AdminService adminService;

    public AdminController(AdminAccess adminAccess, AdminService adminService) {
        this.adminAccess = adminAccess;
        this.adminService = adminService;
    }

    public record AdminMe(String email, boolean admin) {
    }

    @Operation(summary = "Confirmer l'accès administrateur",
            description = "200 si le développeur connecté est administrateur, 403 sinon.")
    @GetMapping("/me")
    public Mono<AdminMe> me() {
        return adminAccess.exigerAdmin().map(compte -> new AdminMe(compte.getEmail(), true));
    }

    @Operation(summary = "Vue d'ensemble", description = "Statistiques globales d'utilisation de la plateforme.")
    @GetMapping("/overview")
    public Mono<AdminService.Overview> overview() {
        return adminAccess.exigerAdmin().then(adminService.overview());
    }

    @Operation(summary = "Lister les développeurs",
            description = "Tous les développeurs avec plan, statut, applications, clés actives et % de consommation.")
    @GetMapping("/developers")
    public Mono<List<AdminService.DeveloperRow>> developpeurs() {
        return adminAccess.exigerAdmin().then(adminService.developpeurs().collectList());
    }

    @Operation(summary = "Détail d'un développeur",
            description = "Applications créées et clés API (statut) du développeur.")
    @GetMapping("/developers/{id}")
    public Mono<AdminService.DeveloperDetail> detail(@PathVariable UUID id) {
        return adminAccess.exigerAdmin().then(adminService.detail(id));
    }

    @Operation(summary = "Lister toutes les applications de la plateforme",
            description = "Vue globale (tous développeurs confondus) des applications enregistrées : "
                    + "nom, version, cycle de vie, callback configuré, développeur propriétaire.")
    @GetMapping("/applications")
    public Mono<List<AdminService.ApplicationRow>> applications() {
        return adminAccess.exigerAdmin().then(adminService.applications().collectList());
    }

    @Operation(summary = "Track des requêtes facturables d'un développeur",
            description = "Historique paginé des requêtes FACTURABLES (KNL_CORE + BUSINESS_CORE). Filtres : "
                    + "catégorie, méthode HTTP, période (JOUR/SEMAINE/MOIS), statut (OK/ERREUR).")
    @GetMapping("/developers/{id}/requests")
    public Mono<AdminService.RequetePage> track(
            @PathVariable UUID id,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String methode,
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int taille) {
        return adminAccess.exigerAdmin().then(adminService.track(id, categorie, methode, periode, statut, page, taille));
    }

    @Operation(summary = "Série temporelle des requêtes de la plateforme",
            description = "Évolution des requêtes (total / facturables / erreurs) agrégée sur toute la plateforme. "
                    + "Période : JOUR (24h par heure), SEMAINE (7j), MOIS (30j), ANNEE (12 mois).")
    @GetMapping("/stats/requests-timeseries")
    public Mono<List<AdminService.TimeseriesPoint>> requestsTimeseries(
            @RequestParam(defaultValue = "MOIS") String periode) {
        return adminAccess.exigerAdmin().then(adminService.requestsTimeseries(periode));
    }

    @Operation(summary = "Top applications par requêtes",
            description = "Applications les plus sollicitées (nombre de requêtes réelles) sur la période.")
    @GetMapping("/stats/top-applications")
    public Mono<List<AdminService.TopApp>> topApplications(
            @RequestParam(defaultValue = "MOIS") String periode,
            @RequestParam(defaultValue = "5") int limite) {
        return adminAccess.exigerAdmin().then(adminService.topApplications(periode, limite));
    }

    @Operation(summary = "Activité récente",
            description = "Flux des derniers événements réels : inscriptions de développeurs et clés API.")
    @GetMapping("/stats/activity")
    public Mono<List<AdminService.ActivityItem>> activity(@RequestParam(defaultValue = "12") int limite) {
        return adminAccess.exigerAdmin().then(adminService.activiteRecente(limite));
    }

    @Operation(summary = "Trafic global de la plateforme",
            description = "Flux de TOUTES les requêtes (tous développeurs/applications), filtrable par développeur, "
                    + "application, facturable (API Business Core / Application), statut (OK/ERREUR), période "
                    + "(1h/24h/7j/30j) et recherche d'endpoint. Paginé.")
    @GetMapping("/traffic")
    public Mono<AdminService.TraficPage> trafic(
            @RequestParam(required = false) UUID developer,
            @RequestParam(required = false) UUID application,
            @RequestParam(required = false) Boolean facturable,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) String recherche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int taille) {
        return adminAccess.exigerAdmin()
                .then(adminService.trafic(developer, application, facturable, statut, periode, recherche, page, taille));
    }

    @Operation(summary = "Facturation / comptabilité",
            description = "Répartition des plans, chiffre d'affaires théorique mensuel et encaissé réel.")
    @GetMapping("/billing")
    public Mono<AdminService.BillingSummary> billing() {
        return adminAccess.exigerAdmin().then(adminService.billing());
    }

    @Operation(summary = "Achats de forfaits (transactions)",
            description = "Historique réel des changements de plan / paiements. Filtre statut : EN_ATTENTE, CONFIRME, REFUSE.")
    @GetMapping("/transactions")
    public Mono<List<AdminService.TransactionRow>> transactions(@RequestParam(required = false) String statut) {
        return adminAccess.exigerAdmin().then(adminService.transactions(statut));
    }

    @Operation(summary = "Série temporelle des revenus encaissés",
            description = "Revenus réellement encaissés (paiements confirmés) agrégés selon la période.")
    @GetMapping("/stats/revenue-timeseries")
    public Mono<List<AdminService.TimeseriesPoint>> revenueTimeseries(
            @RequestParam(defaultValue = "MOIS") String periode) {
        return adminAccess.exigerAdmin().then(adminService.revenueTimeseries(periode));
    }

    @Operation(summary = "Tarification des plans", description = "Prix, quota et devise courants de chaque plan.")
    @GetMapping("/pricing")
    public Mono<List<AdminService.PricingRow>> pricing() {
        return adminAccess.exigerAdmin().thenReturn(adminService.pricing());
    }

    public record DefinirTarifRequest(long quotaMensuel, long prixMensuel, String devise, Long applicationsMax,
                                      String libelle) {
    }

    @Operation(summary = "Créer ou modifier un forfait",
            description = "L'administrateur fixe libellé / prix / quota / devise / limite d'applications d'un plan "
                    + "(création si le code n'existe pas) — effet immédiat et persisté. applicationsMax null ou < 0 = illimité.")
    @PostMapping("/pricing/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> definirTarif(@PathVariable String code, @RequestBody DefinirTarifRequest req) {
        long appsMax = req.applicationsMax() == null ? -1 : req.applicationsMax();
        return adminAccess.exigerAdmin()
                .then(adminService.definirTarif(code, req.quotaMensuel(), req.prixMensuel(), req.devise(),
                        appsMax, req.libelle()));
    }

    @Operation(summary = "Supprimer un forfait",
            description = "Suppression définitive d'un forfait. Refusée pour FREE (plan par défaut) ou si des développeurs y sont abonnés.")
    @DeleteMapping("/pricing/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> supprimerPlan(@PathVariable String code) {
        return adminAccess.exigerAdmin().then(adminService.supprimerPlan(code));
    }

    @Operation(summary = "Bloquer un développeur",
            description = "Suspend le développeur : ses clés API cessent immédiatement de fonctionner.")
    @PostMapping("/developers/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> bloquer(@PathVariable UUID id) {
        return adminAccess.exigerAdmin().then(adminService.bloquerDeveloppeur(id));
    }

    @Operation(summary = "Débloquer un développeur", description = "Réactive un développeur suspendu.")
    @PostMapping("/developers/{id}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> debloquer(@PathVariable UUID id) {
        return adminAccess.exigerAdmin().then(adminService.debloquerDeveloppeur(id));
    }

    @Operation(summary = "Révoquer une clé API",
            description = "Révocation immédiate et définitive d'une clé, quel que soit le développeur.")
    @PostMapping("/keys/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> revoquerCle(@PathVariable UUID id) {
        return adminAccess.exigerAdmin().then(adminService.revoquerCle(id));
    }

    public record ChangerPlanRequest(String plan) {
    }

    @Operation(summary = "Forcer le forfait d'un développeur",
            description = "L'administrateur change le forfait d'un développeur — effet immédiat sur son quota.")
    @PostMapping("/developers/{id}/plan")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> changerPlan(@PathVariable UUID id, @RequestBody ChangerPlanRequest req) {
        return adminAccess.exigerAdmin().then(adminService.changerPlan(id, req.plan()));
    }

}
