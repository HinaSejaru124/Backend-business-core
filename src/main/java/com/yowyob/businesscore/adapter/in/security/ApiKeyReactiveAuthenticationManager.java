package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Valide une clé Business Core et construit le {@link BusinessContext}.
 *
 * <p>{@code X-BC-Client-Id} est l'identifiant <b>stable du développeur</b> (le {@code developerId}
 * exposé par {@code GET /v1/auth/me}, immuable, jamais rattaché à une clé). {@code X-BC-Api-Key} est le
 * secret d'une entreprise précise : chaque entreprise n'a jamais plus d'une clé {@code ACTIVE} à la fois
 * (imposé à la création, cf. {@link ApiKeyService#creer}). Le client_id ne pointe donc pas vers une
 * ligne unique de {@code api_key} : on charge le développeur, puis on confronte le secret aux hachés de
 * ses clés actives (une par entreprise qu'il possède) jusqu'à trouver la correspondance.
 *
 * <p>Si le compte n'a pas encore de {@code kernel_tenant_id} (jamais connecté), l'accès est refusé avec
 * un message explicite ({@link EspaceNonLieException}). La date de dernière utilisation de la clé est
 * mise à jour à chaque validation réussie.
 */
@Component
public class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ApiKeyRepository apiKeyRepository;
    private final DeveloperAccountRepository developerRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyService apiKeyService;

    public ApiKeyReactiveAuthenticationManager(ApiKeyRepository apiKeyRepository,
                                               DeveloperAccountRepository developerRepository,
                                               EntrepriseRepository entrepriseRepository,
                                               PasswordEncoder passwordEncoder,
                                               ApiKeyService apiKeyService) {
        this.apiKeyRepository = apiKeyRepository;
        this.developerRepository = developerRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String secret = (String) token.getCredentials();
        UUID developerId = parseUuid(token.getClientId());
        if (developerId == null) {
            return Mono.error(new BadCredentialsException("Clé Business Core invalide"));
        }

        return developerRepository.findById(developerId)
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")))
                .flatMap(account -> {
                    if (account.getKernelTenantId() == null) {
                        return Mono.error(new EspaceNonLieException(
                                "Clé valide mais espace non lié : connectez-vous une fois via "
                                        + "POST /v1/auth/login pour activer votre clé."));
                    }
                    return resoudreCle(account, secret)
                            .flatMap(cle -> resoudreEntreprise(cle, account)
                                    .flatMap(businessId -> {
                                        Authentication auth = authentifie(
                                                account, token.getOnBehalfOf(), cle.getId(), businessId);
                                        return apiKeyService.marquerUtilisee(cle.getId()).thenReturn(auth);
                                    }));
                });
    }

    /** Confronte le secret aux hachés des clés ACTIVE du développeur (une par entreprise). */
    private Mono<ApiKeyEntity> resoudreCle(DeveloperAccountEntity account, String secret) {
        return apiKeyRepository.findByDeveloperIdAndStatus(account.getId(), ApiKeyEntity.STATUT_ACTIVE)
                .filter(cle -> passwordEncoder.matches(secret, cle.getKeyHash()))
                .next()
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")));
    }

    /**
     * Défense en profondeur : l'entreprise résolue doit réellement appartenir au tenant de ce
     * développeur et ne pas être fermée — ne devrait jamais échouer en usage normal (une entreprise ne
     * change pas de tenant après coup), mais protège contre toute anomalie de données.
     *
     * <p>À ce stade, l'authentification n'a pas encore abouti : {@link BusinessContextWebFilter} (qui
     * peuple normalement le Reactor Context lu par le pool R2DBC pour positionner
     * {@code app.current_tenant}) ne s'exécute qu'après. Sans ça, la RLS (FORCE) de {@code entreprise}
     * masquerait la ligne même pour une clé valide. Le tenant étant déjà connu (compte développeur,
     * table sans RLS), on l'injecte nous-mêmes dans le Reactor Context, le temps de cette requête.
     */
    private Mono<UUID> resoudreEntreprise(ApiKeyEntity cle, DeveloperAccountEntity account) {
        BusinessContext contextePreliminaire = new BusinessContext(
                account.getKernelTenantId(), null, Set.of(), null, UUID.randomUUID().toString(), Locale.getDefault());
        return entrepriseRepository.findById(cle.getEntrepriseId())
                .filter(entreprise -> account.getKernelTenantId().equals(entreprise.getTenantId()))
                .filter(entreprise -> !"FERMEE".equals(entreprise.getCycleVie()))
                .map(entreprise -> entreprise.getId())
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")))
                .contextWrite(ctx -> BusinessContextHolder.withContext(ctx, contextePreliminaire));
    }

    private Authentication authentifie(DeveloperAccountEntity account, String onBehalfOf, UUID apiKeyId,
                                       UUID businessId) {
        BusinessContext context = new BusinessContext(
                account.getKernelTenantId(),
                parseActor(onBehalfOf),
                Set.of(),
                businessId,
                UUID.randomUUID().toString(),
                Locale.getDefault());
        // developerId + plan portés par le token : lus par la porte de quota et le comptage mensuel.
        return ApiKeyAuthenticationToken.authenticated(context, apiKeyId, account.getId(), account.getPlan());
    }

    private UUID parseActor(String onBehalfOf) {
        return parseUuid(onBehalfOf);
    }

    private UUID parseUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
