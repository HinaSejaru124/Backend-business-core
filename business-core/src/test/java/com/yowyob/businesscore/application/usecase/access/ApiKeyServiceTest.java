package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseEntity;
import com.yowyob.businesscore.adapter.out.persistence.enterprise.EntrepriseRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock ApiKeyRepository repository;
    @Mock EntrepriseRepository entrepriseRepository;
    @Mock DeveloperAccountRepository developerRepository;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private ApiKeyService service() {
        return new ApiKeyService(repository, entrepriseRepository, developerRepository, encoder);
    }

    private static EntrepriseEntity entrepriseAvecTenant(UUID id, UUID tenantId) {
        return EntrepriseEntity.nouveau(
                id, tenantId, UUID.randomUUID(), UUID.randomUUID(), 1, null, null, null, "Pharma Test", "ACTIVE");
    }

    @Test
    @DisplayName("crée une clé (secret) et n'en persiste que le haché")
    void creer_ok() {
        ApiKeyService service = service();
        UUID dev = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        when(developerRepository.findById(dev)).thenReturn(Mono.just(
                DeveloperAccountEntity.nouveau(dev, "dev@x.co", tenantId, null, null, null, "FREE")));
        when(entrepriseRepository.findById(entrepriseId)).thenReturn(Mono.just(
                entrepriseAvecTenant(entrepriseId, tenantId)));
        when(repository.countByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Mono.just(0L));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.creer(dev, entrepriseId, "Prod"))
                .assertNext(cle -> {
                    assertThat(cle.secret()).isNotBlank();
                    assertThat(cle.name()).isEqualTo("Prod");
                    assertThat(cle.entrepriseId()).isEqualTo(entrepriseId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("refuse la création si l'entreprise a déjà une clé active (409)")
    void creer_deja_une_cle_active() {
        ApiKeyService service = service();
        UUID dev = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        when(developerRepository.findById(dev)).thenReturn(Mono.just(
                DeveloperAccountEntity.nouveau(dev, "dev@x.co", tenantId, null, null, null, "FREE")));
        when(entrepriseRepository.findById(entrepriseId)).thenReturn(Mono.just(
                entrepriseAvecTenant(entrepriseId, tenantId)));
        when(repository.countByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(service.creer(dev, entrepriseId, "Doublon"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ProblemException.class))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("refuse la création sans entrepriseId (400)")
    void creer_sans_entreprise_refuse() {
        ApiKeyService service = service();
        StepVerifier.create(service.creer(UUID.randomUUID(), null, "Sans business"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ProblemException.class))
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("révoque la clé active de l'entreprise")
    void revoquer_ok() {
        ApiKeyService service = service();
        UUID dev = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, dev, entrepriseId, "hash", "Prod");
        when(repository.findByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.just(cle));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.revoquer(entrepriseId))
                .assertNext(k -> assertThat(k.getStatus()).isEqualTo(ApiKeyEntity.STATUT_REVOKED))
                .verifyComplete();
    }

    @Test
    @DisplayName("révoquer sans clé active → introuvable")
    void revoquer_sans_cle_active() {
        ApiKeyService service = service();
        UUID entrepriseId = UUID.randomUUID();
        when(repository.findByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.revoquer(entrepriseId))
                .expectError(ProblemException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("marque la clé utilisée (last_used_at)")
    void marquer_utilisee() {
        ApiKeyService service = service();
        UUID keyId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, UUID.randomUUID(), UUID.randomUUID(), "hash", "Prod");
        when(repository.findById(keyId)).thenReturn(Mono.just(cle));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.marquerUtilisee(keyId)).verifyComplete();

        verify(repository).save(any());
        assertThat(cle.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("trouve la clé active d'une entreprise")
    void trouverActive() {
        ApiKeyService service = service();
        UUID dev = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        when(repository.findByEntrepriseIdAndStatus(entrepriseId, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(Flux.just(ApiKeyEntity.nouveau(UUID.randomUUID(), dev, entrepriseId, "h", "A")));

        StepVerifier.create(service.trouverActive(entrepriseId))
                .assertNext(cle -> assertThat(cle.getEntrepriseId()).isEqualTo(entrepriseId))
                .verifyComplete();
        verify(repository).findByEntrepriseIdAndStatus(eq(entrepriseId), eq(ApiKeyEntity.STATUT_ACTIVE));
    }
}
