package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyEntity;
import com.yowyob.businesscore.adapter.out.persistence.apikey.ApiKeyRepository;
import com.yowyob.businesscore.application.error.ProblemException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private ApiKeyService service(int max) {
        return new ApiKeyService(repository, encoder, max);
    }

    @Test
    @DisplayName("crée une clé (prefix + secret) et n'en persiste que le haché")
    void creer_ok() {
        ApiKeyService service = service(5);
        UUID dev = UUID.randomUUID();
        when(repository.countByDeveloperIdAndStatus(dev, ApiKeyEntity.STATUT_ACTIVE)).thenReturn(Mono.just(0L));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.creer(dev, "Prod"))
                .assertNext(cle -> {
                    assertThat(cle.prefix()).startsWith("bck_");
                    assertThat(cle.secret()).isNotBlank();
                    assertThat(cle.name()).isEqualTo("Prod");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("refuse la création au-delà de la limite de clés actives (409)")
    void creer_limite_atteinte() {
        ApiKeyService service = service(5);
        UUID dev = UUID.randomUUID();
        when(repository.countByDeveloperIdAndStatus(dev, ApiKeyEntity.STATUT_ACTIVE)).thenReturn(Mono.just(5L));

        StepVerifier.create(service.creer(dev, "Trop"))
                .expectErrorSatisfies(ex -> assertThat(ex).isInstanceOf(ProblemException.class))
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("révoque une clé possédée par le développeur")
    void revoquer_ok() {
        ApiKeyService service = service(5);
        UUID dev = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, dev, "bck_x", "hash", "Prod");
        when(repository.findById(keyId)).thenReturn(Mono.just(cle));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.revoquer(dev, keyId))
                .assertNext(k -> assertThat(k.getStatus()).isEqualTo(ApiKeyEntity.STATUT_REVOKED))
                .verifyComplete();
    }

    @Test
    @DisplayName("révoquer une clé d'un autre développeur → introuvable")
    void revoquer_autre_dev() {
        ApiKeyService service = service(5);
        UUID keyId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, UUID.randomUUID(), "bck_x", "hash", "Prod");
        when(repository.findById(keyId)).thenReturn(Mono.just(cle));

        StepVerifier.create(service.revoquer(UUID.randomUUID(), keyId))
                .expectError(ProblemException.class)
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("marque la clé utilisée (last_used_at)")
    void marquer_utilisee() {
        ApiKeyService service = service(5);
        UUID keyId = UUID.randomUUID();
        ApiKeyEntity cle = ApiKeyEntity.nouveau(keyId, UUID.randomUUID(), "bck_x", "hash", "Prod");
        when(repository.findById(keyId)).thenReturn(Mono.just(cle));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.marquerUtilisee(keyId)).verifyComplete();

        verify(repository).save(any());
        assertThat(cle.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("liste les clés d'un développeur")
    void lister() {
        ApiKeyService service = service(5);
        UUID dev = UUID.randomUUID();
        when(repository.findByDeveloperIdAndStatus(dev, ApiKeyEntity.STATUT_ACTIVE))
                .thenReturn(reactor.core.publisher.Flux.just(
                ApiKeyEntity.nouveau(UUID.randomUUID(), dev, "bck_1", "h", "A")));

        StepVerifier.create(service.lister(dev)).expectNextCount(1).verifyComplete();
        verify(repository).findByDeveloperIdAndStatus(eq(dev), eq(ApiKeyEntity.STATUT_ACTIVE));
    }
}
