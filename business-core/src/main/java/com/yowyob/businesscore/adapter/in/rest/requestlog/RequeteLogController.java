package com.yowyob.businesscore.adapter.in.rest.requestlog;

import com.yowyob.businesscore.adapter.out.persistence.requestlog.RequeteLogRepository;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Journal détaillé des requêtes (console développeur, onglet Track) — consultation exclusivement JWT.
 * Trois catégories réelles : {@code KNL_CORE} (Business Core → Kernel), {@code BUSINESS_CORE} (requête
 * reçue par Business Core) et {@code APP} (requête propre du backend du développeur, rapportée via
 * l'ingestion de télémétrie). Le champ {@code facturable} distingue ce qui compte dans le quota
 * (KNL_CORE + BUSINESS_CORE runtime) du reste. Filtres serveur : catégorie, méthode, période, statut,
 * facturable (cf. DOCUMENTATION-REQUETES.md).
 */
@Tag(name = "Audit", description = "Journal détaillé des requêtes par catégorie")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/requetes")
public class RequeteLogController {

    private static final int TAILLE_MAX = 100;

    private final RequeteLogRepository repository;

    public RequeteLogController(RequeteLogRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Lister les requêtes journalisées",
            description = "Filtres optionnels : catégorie (KNL_CORE / BUSINESS_CORE / APP), méthode HTTP, "
                    + "période (JOUR / SEMAINE / MOIS), statut (OK / ERREUR), facturable. Paginé, anti-chronologique.")
    @GetMapping
    public Mono<RequeteLogPageResponse> lister(
            @Parameter(description = "KNL_CORE, BUSINESS_CORE ou APP — omis pour toutes") @RequestParam(required = false) String categorie,
            @Parameter(description = "Méthode HTTP exacte (GET, POST, …)") @RequestParam(required = false) String methode,
            @Parameter(description = "JOUR, SEMAINE ou MOIS — omis pour tout l'historique") @RequestParam(required = false) String periode,
            @Parameter(description = "OK (succès <400) ou ERREUR (>=400)") @RequestParam(required = false) String statut,
            @Parameter(description = "true = facturables uniquement") @RequestParam(required = false) Boolean facturable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int taille) {
        int tailleEffective = Math.min(Math.max(1, taille), TAILLE_MAX);
        int pageEffective = Math.max(0, page);
        long decalage = (long) pageEffective * tailleEffective;

        String cat = vide(categorie) ? null : categorie.trim().toUpperCase(Locale.ROOT);
        String meth = vide(methode) ? null : methode.trim().toUpperCase(Locale.ROOT);
        Instant depuis = bornePeriode(periode);
        Integer erreurFlag = flagStatut(statut);
        Integer facturableFlag = facturable == null ? null : (facturable ? 1 : 0);

        return BusinessContextHolder.currentContext().flatMap(ctx -> {
            var items = repository
                    .pageFiltree(ctx.tenantId(), cat, meth, depuis, erreurFlag, facturableFlag, tailleEffective, decalage)
                    .map(RequeteLogResponse::depuis)
                    .collectList();
            var total = repository.countFiltree(ctx.tenantId(), cat, meth, depuis, erreurFlag, facturableFlag);
            return Mono.zip(items, total)
                    .map(t -> new RequeteLogPageResponse(t.getT1(), t.getT2(), pageEffective, tailleEffective));
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
            case "JOUR" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "SEMAINE" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "MOIS" -> Instant.now().minus(30, ChronoUnit.DAYS);
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
}
