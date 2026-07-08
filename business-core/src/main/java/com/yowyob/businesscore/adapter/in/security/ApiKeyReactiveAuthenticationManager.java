package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
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
 * Valide une clé API (table {@code api_key}) et construit le {@link BusinessContext}.
 *
 * <p>Chaîne : {@code findByPrefix} (X-BC-Client-Id) → clé ACTIVE → secret vérifié contre le hash →
 * compte développeur → tenant = {@code kernel_tenant_id}. Le tenant est donc le tenant kernel, comme
 * pour le flux JWT : les deux modes d'auth résolvent le même espace (cohérence RLS).
 *
 * <p>Si le compte n'a pas encore de {@code kernel_tenant_id} (jamais connecté), l'accès est refusé avec
 * un message explicite ({@link EspaceNonLieException}). La date de dernière utilisation de la clé est
 * mise à jour à chaque validation réussie.
 */
@Component
public class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final ApiKeyRepository apiKeyRepository;
    private final DeveloperAccountRepository developerRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyService apiKeyService;

    public ApiKeyReactiveAuthenticationManager(ApiKeyRepository apiKeyRepository,
                                               DeveloperAccountRepository developerRepository,
                                               PasswordEncoder passwordEncoder,
                                               ApiKeyService apiKeyService) {
        this.apiKeyRepository = apiKeyRepository;
        this.developerRepository = developerRepository;
        this.passwordEncoder = passwordEncoder;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String prefix = token.getClientId();
        String secret = (String) token.getCredentials();

        return apiKeyRepository.findByPrefix(prefix)
                .filter(cle -> cle.estActive())
                .filter(cle -> passwordEncoder.matches(secret, cle.getKeyHash()))
                .flatMap(cle -> construireContexte(cle, token.getOnBehalfOf()))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")));
    }

    private Mono<Authentication> construireContexte(ApiKeyEntity cle, String onBehalfOf) {
        return developerRepository.findById(cle.getDeveloperId())
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .flatMap(account -> {
                    if (account.getKernelTenantId() == null) {
                        return Mono.error(new EspaceNonLieException(
                                "Clé valide mais espace non lié : connectez-vous une fois via "
                                        + "POST /v1/auth/login pour activer votre clé."));
                    }
                    Authentication auth = authentifie(account, onBehalfOf, cle.getId());
                    return apiKeyService.marquerUtilisee(cle.getId()).thenReturn(auth);
                })
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")));
    }

    private Authentication authentifie(DeveloperAccountEntity account, String onBehalfOf, UUID apiKeyId) {
        BusinessContext context = new BusinessContext(
                account.getKernelTenantId(),
                parseActor(onBehalfOf),
                Set.of(),
                null,
                UUID.randomUUID().toString(),
                Locale.getDefault());
        return ApiKeyAuthenticationToken.authenticated(context, apiKeyId);
    }

    private UUID parseActor(String onBehalfOf) {
        if (onBehalfOf == null || onBehalfOf.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(onBehalfOf);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
