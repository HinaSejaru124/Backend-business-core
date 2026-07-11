package com.yowyob.businesscore.adapter.in.security;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseEntity;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
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
import reactor.core.publisher.Flux;
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
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ApiKeyService apiKeyService;

    private ApiKeyReactiveAuthenticationManager manager() {
        return new ApiKeyReactiveAuthenticationManager(
                apiKeyRepository, developerRepository, entrepriseRepository, passwordEncoder, apiKeyService);
    }

    private DeveloperAccountEntity compte(UUID id, UUID tenantId) {
        DeveloperAccountEntity e = DeveloperAccountEntity.nouveau(id, "dev@x.co", tenantId, null, null, null, "FREE");
        return e;
    }

    private EntrepriseEntity entreprise(UUID id, UUID tenantId, String cycleVie) {
        return EntrepriseEntity.nouveau(id, tenantId, UUID.randomUUID(), UUID.randomUUID(), 1,
                null, null, null, "Pharma Test", cycleVie);
    }

    @Test
    @DisplayName("clé valide → contexte avec tenant + businessId de l'entreprise, last_used mis à jour")
    void auth_ok_tenant_kernel() {
        UUID developerId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, developerId, entrepriseId, "hash", "Prod");
        DeveloperAccountEntity account = compte(developerId, tenantId);

        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));
        when(apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.just(cle));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(entrepriseRepository.findById(entrepriseId))
                .thenReturn(Mono.just(entreprise(entrepriseId, tenantId, "ACTIVE")));
        when(apiKeyService.marquerUtilisee(keyId)).thenReturn(Mono.empty());

        Authentication input = ApiKeyAuthenticationToken.unauthenticated(developerId.toString(), "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .assertNext(auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    BusinessContext ctx = (BusinessContext) auth.getPrincipal();
                    assertThat(ctx.tenantId()).isEqualTo(tenantId);
                    assertThat(ctx.businessId()).isEqualTo(entrepriseId);
                })
                .verifyComplete();

        verify(apiKeyService).marquerUtilisee(eq(keyId));
    }

    @Test
    @DisplayName("clé révoquée → aucune candidate active, rejet")
    void auth_cle_revoquee() {
        UUID developerId = UUID.randomUUID();
        DeveloperAccountEntity account = compte(developerId, UUID.randomUUID());

        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));
        when(apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.empty());

        Authentication input = ApiKeyAuthenticationToken.unauthenticated(developerId.toString(), "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("compte sans tenant kernel lié → 401 explicite (EspaceNonLieException)")
    void auth_espace_non_lie() {
        UUID developerId = UUID.randomUUID();
        DeveloperAccountEntity account = compte(developerId, null);

        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));

        Authentication input = ApiKeyAuthenticationToken.unauthenticated(developerId.toString(), "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(EspaceNonLieException.class)
                .verify();
    }

    @Test
    @DisplayName("secret invalide (aucune clé active ne matche) → rejet")
    void auth_secret_invalide() {
        UUID developerId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(UUID.randomUUID(), developerId, UUID.randomUUID(), "hash", "Prod");
        DeveloperAccountEntity account = compte(developerId, tenantId);

        when(developerRepository.findById(developerId)).thenReturn(Mono.just(account));
        when(apiKeyRepository.findByDeveloperIdAndStatus(developerId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.just(cle));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        Authentication input = ApiKeyAuthenticationToken.unauthenticated(developerId.toString(), "mauvais", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("client_id inconnu (développeur introuvable) → rejet")
    void auth_developpeur_inconnu() {
        UUID developerId = UUID.randomUUID();
        when(developerRepository.findById(developerId)).thenReturn(Mono.empty());

        Authentication input = ApiKeyAuthenticationToken.unauthenticated(developerId.toString(), "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    @DisplayName("client_id mal formé (pas un UUID) → rejet")
    void auth_client_id_invalide() {
        Authentication input = ApiKeyAuthenticationToken.unauthenticated("pas-un-uuid", "secret", null);

        StepVerifier.create(manager().authenticate(input))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
