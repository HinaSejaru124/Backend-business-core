package com.yowyob.businesscore.adapter.in.rest.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Portail développeur — gestion des clés API (famille Accès). Le développeur courant est résolu depuis
 * le tenant du {@link BusinessContext} (posé par l'authentification). Le secret d'une clé n'est renvoyé
 * qu'à la création.
 * <ul>
 *   <li>{@code POST   /v1/api-keys} — créer une clé (secret affiché une fois) ;</li>
 *   <li>{@code GET    /v1/api-keys} — lister ses clés (sans secret) ;</li>
 *   <li>{@code PATCH  /v1/api-keys/{id}} — renommer ;</li>
 *   <li>{@code POST   /v1/api-keys/{id}:revoke} — révoquer (immédiat).</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final DeveloperAccountRepository developerRepository;

    public ApiKeyController(ApiKeyService apiKeyService,
                           DeveloperAccountRepository developerRepository) {
        this.apiKeyService = apiKeyService;
        this.developerRepository = developerRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CleApiCreeeResponse> creer(@RequestBody(required = false) CreerCleRequest requete) {
        String nom = requete == null ? null : requete.name();
        return developerCourant()
                .flatMap(developerId -> apiKeyService.creer(developerId, nom))
                .map(CleApiCreeeResponse::depuis);
    }

    @GetMapping
    public Flux<CleApiResponse> lister() {
        return developerCourant()
                .flatMapMany(apiKeyService::lister)
                .map(CleApiResponse::depuis);
    }

    @PatchMapping("/{id}")
    public Mono<CleApiResponse> renommer(@PathVariable UUID id,
                                         @Valid @RequestBody RenommerCleRequest requete) {
        return developerCourant()
                .flatMap(developerId -> apiKeyService.renommer(developerId, id, requete.name()))
                .map(CleApiResponse::depuis);
    }

    @PostMapping("/{id}:revoke")
    public Mono<CleApiResponse> revoquer(@PathVariable UUID id) {
        return developerCourant()
                .flatMap(developerId -> apiKeyService.revoquer(developerId, id))
                .map(CleApiResponse::depuis);
    }

    /** Résout le compte développeur courant depuis le tenant kernel du contexte. */
    private Mono<UUID> developerCourant() {
        return BusinessContextHolder.currentContext()
                .switchIfEmpty(Mono.error(ProblemException.forbidden("Contexte d'authentification absent")))
                .map(ctx -> ctx.tenantId())
                .flatMap(developerRepository::findByKernelTenantId)
                .map(account -> account.getId())
                .switchIfEmpty(Mono.error(ProblemException.notFound("Compte développeur introuvable")));
    }
}
