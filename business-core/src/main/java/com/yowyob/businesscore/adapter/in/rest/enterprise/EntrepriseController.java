package com.yowyob.businesscore.adapter.in.rest.enterprise;

import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.enterprise.EntrepriseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API REST — Brique 3 (Entreprise), version minimale fournie par la feature Opérations pour
 * l'exécution de bout en bout (à compléter/fusionner avec Dev 3). Routes (cf. OpenAPI) :
 * <ul>
 *   <li>{@code POST /v1/businesses} — créer ;</li>
 *   <li>{@code GET  /v1/businesses} — lister ;</li>
 *   <li>{@code GET  /v1/businesses/{businessId}} — consulter ;</li>
 *   <li>{@code PUT  /v1/businesses/{businessId}/lifecycle} — changer le cycle de vie.</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/businesses")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EntrepriseResponse> creer(@Valid @RequestBody CreerEntrepriseRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.creer(
                        requete.typeId(), requete.versionNumber(), requete.nom(),
                        requete.organizationId(), ctx))
                .map(EntrepriseResponse::depuis);
    }

    @GetMapping
    public Flux<EntrepriseResponse> lister() {
        return BusinessContextHolder.currentContext()
                .flatMapMany(entrepriseService::lister)
                .map(EntrepriseResponse::depuis);
    }

    @GetMapping("/{businessId}")
    public Mono<EntrepriseResponse> trouver(@PathVariable UUID businessId) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.trouver(businessId, ctx))
                .map(EntrepriseResponse::depuis);
    }

    @PutMapping("/{businessId}/lifecycle")
    public Mono<EntrepriseResponse> changerCycleVie(@PathVariable UUID businessId,
                                                    @Valid @RequestBody ChangerCycleVieRequest requete) {
        return BusinessContextHolder.currentContext()
                .flatMap(ctx -> entrepriseService.changerCycleVie(businessId, requete.cycleVie(), ctx))
                .map(EntrepriseResponse::depuis);
    }
}
