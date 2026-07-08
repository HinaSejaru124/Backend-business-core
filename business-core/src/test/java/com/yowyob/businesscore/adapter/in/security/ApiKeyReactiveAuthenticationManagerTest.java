package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyReactiveAuthenticationManagerTest {

    @Mock ApiKeyRepository apiKeyRepository;
    @Mock DeveloperAccountRepository developerRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApiKeyService apiKeyService;

    private ApiKeyReactiveAuthenticationManager manager() {
        return new ApiKeyReactiveAuthenticationManager(
                apiKeyRepository, developerRepository, passwordEncoder, apiKeyService);
    }

    private DeveloperAccountEntity compte(UUID tenantId) {
        return DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", tenantId, null, null, "FREE");
    }

    @Test
    @DisplayName("clé valide → contexte avec tenant = kernel_tenant_id + last_used mis à jour")
    void auth_ok_tenant_kernel() {
        UUID developerId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, developerId, "bck_a", "hash", "Prod");
        DeveloperAccountEntity account = compte(tenantId);

        when(apiKeyRepository.findByPrefix("bck_a")).thenReturn(Mono.just(cle));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));
        when(apiKeyService.marquerUtilisee(keyId)).thenReturn(Mono.empty());

        Authentication input = ApiKeyAuthenticationToken.unauthenticated("bck_a", "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .assertNext(auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    BusinessContext ctx = (BusinessContext) auth.getPrincipal();
                    assertThat(ctx.tenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();

        verify(apiKeyService).marquerUtilisee(eq(keyId));
    }

    @Test
    @DisplayName("clé révoquée → rejet")
    void auth_cle_revoquee() {
        ApiKeyEntity cle = ApiKeyEntity.nouveau(UUID.randomUUID(), UUID.randomUUID(), "bck_r", "hash", "Prod");
        cle.setStatus(ApiKeyEntity.STATUT_REVOKED);
        when(apiKeyRepository.findByPrefix("bck_r")).thenReturn(Mono.just(cle));

        Authentication input = ApiKeyAuthenticationToken.unauthenticated("bck_r", "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("compte sans tenant kernel lié → 401 explicite (EspaceNonLieException)")
    void auth_espace_non_lie() {
        UUID developerId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(UUID.randomUUID(), developerId, "bck_n", "hash", "Prod");
        DeveloperAccountEntity account = compte(null);

        when(apiKeyRepository.findByPrefix("bck_n")).thenReturn(Mono.just(cle));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));

        Authentication input = ApiKeyAuthenticationToken.unauthenticated("bck_n", "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(EspaceNonLieException.class)
                .verify();
    }

    @Test
    @DisplayName("secret invalide → rejet")
    void auth_secret_invalide() {
        ApiKeyEntity cle = ApiKeyEntity.nouveau(UUID.randomUUID(), UUID.randomUUID(), "bck_b", "hash", "Prod");
        when(apiKeyRepository.findByPrefix("bck_b")).thenReturn(Mono.just(cle));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        Authentication input = ApiKeyAuthenticationToken.unauthenticated("bck_b", "mauvais", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("préfixe inconnu → rejet")
    void auth_prefix_inconnu() {
        when(apiKeyRepository.findByPrefix("bck_?")).thenReturn(Mono.empty());
        Authentication input = ApiKeyAuthenticationToken.unauthenticated("bck_?", "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
