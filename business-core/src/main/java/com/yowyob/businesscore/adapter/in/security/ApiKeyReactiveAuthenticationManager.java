package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
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
 * Valide la clé Business Core contre {@code developer_account} et construit le {@link BusinessContext}.
 *
 * <p>Le {@code tenantId} provient de l'identité du développeur (jamais du payload). L'acteur asserté
 * (on-behalf-of) est accepté tel que fourni par le backend du développeur (modèle de confiance, A2) ;
 * sa validation fine d'appartenance au tenant et la résolution de ses rôles métier seront complétées
 * avec la brique Acteurs (Dev 3).
 */
@Component
public class ApiKeyReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final DeveloperAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ApiKeyReactiveAuthenticationManager(DeveloperAccountRepository repository,
                                               PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) authentication;
        String clientId = token.getClientId();
        String apiKey = (String) token.getCredentials();

        return repository.findByBcClientId(clientId)
                .filter(account -> "ACTIVE".equals(account.getStatus()))
                .filter(account -> passwordEncoder.matches(apiKey, account.getBcApiKeyHash()))
                .map(account -> {
                    BusinessContext context = new BusinessContext(
                            account.getId(),
                            parseActor(token.getOnBehalfOf()),
                            Set.of(),
                            null,
                            UUID.randomUUID().toString(),
                            Locale.getDefault()
                    );
                    return (Authentication) ApiKeyAuthenticationToken.authenticated(context);
                })
                .switchIfEmpty(Mono.error(new BadCredentialsException("Clé Business Core invalide")));
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
