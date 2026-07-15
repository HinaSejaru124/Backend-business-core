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

/**
 * Journal détaillé des requêtes (console développeur, onglet Audit / Requêtes) — consultation
 * exclusivement JWT (console). Deux catégories réelles : {@code KNL_CORE} (Business Core → Kernel) et
 * {@code BUSINESS_CORE} (toute requête authentifiée reçue par Business Core, clé API OU JWT). Le champ
 * {@code facturable} distingue ce qui compte dans le quota (clé API + Kernel) de la modélisation JWT
 * (design-time), qui n'en consomme jamais. Le backend propre du développeur n'est jamais visible depuis
 * ici (cf. DOCUMENTATION-REQUETES.md).
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
            description = "Filtrable par catégorie (KNL_CORE, BUSINESS_CORE), paginé, tri anti-chronologique.")
    @GetMapping
    public Mono<RequeteLogPageResponse> lister(
            @Parameter(description = "KNL_CORE ou BUSINESS_CORE — omis pour les deux") @RequestParam(required = false) String categorie,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        int tailleEffective = Math.min(Math.max(1, taille), TAILLE_MAX);
        int pageEffective = Math.max(0, page);
        long decalage = (long) pageEffective * tailleEffective;

        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> {
                    var items = (categorie != null && !categorie.isBlank()
                            ? repository.pageParTenantEtCategorie(ctx.tenantId(), categorie, tailleEffective, decalage)
                            : repository.pageParTenant(ctx.tenantId(), tailleEffective, decalage))
                            .map(RequeteLogResponse::depuis)
                            .collectList();
                    var total = categorie != null && !categorie.isBlank()
                            ? repository.countByTenantIdAndCategorie(ctx.tenantId(), categorie)
                            : repository.countByTenantId(ctx.tenantId());
                    return Mono.zip(items, total)
                            .map(t -> new RequeteLogPageResponse(t.getT1(), t.getT2(), pageEffective, tailleEffective));
                });
    }
}
