package com.yowyob.businesscore.application.usecase.auth;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.ResultatLogin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthentificationServiceTest {

    @Mock AuthentifierUtilisateur authentifier;
    @Mock DeveloperAccountRepository repository;

    private ResultatLogin login(String tenantId) {
        return login(tenantId, "jwt-simple");
    }

    private ResultatLogin login(String tenantId, String accessToken) {
        return new ResultatLogin(accessToken, 900, List.of("organizations:write"), List.of(), tenantId, "actor-1");
    }

    @Test
    @DisplayName("connexion : lie le tenant kernel au compte si absent")
    void connecter_lie_tenant_absent() {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID tenantId = UUID.randomUUID();
        DeveloperAccountEntity account =
                DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", null, null, null, null, "FREE");

        when(authentifier.login("dev@x.co", "pw")).thenReturn(Mono.just(login(tenantId.toString())));
        when(repository.findByEmail("dev@x.co")).thenReturn(Mono.just(account));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.connecter("dev@x.co", "pw"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<DeveloperAccountEntity> captor = ArgumentCaptor.forClass(DeveloperAccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKernelTenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("connexion : réaligne le tenant si le contexte kernel a changé")
    void connecter_synchronise_tenant_different() {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID ancien = UUID.randomUUID();
        UUID nouveau = UUID.randomUUID();
        DeveloperAccountEntity account =
                DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", ancien, null, null, null, "FREE");

        when(authentifier.login("dev@x.co", "pw")).thenReturn(Mono.just(login(nouveau.toString())));
        when(repository.findByEmail("dev@x.co")).thenReturn(Mono.just(account));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.connecter("dev@x.co", "pw"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<DeveloperAccountEntity> captor = ArgumentCaptor.forClass(DeveloperAccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKernelTenantId()).isEqualTo(nouveau);
    }

    @Test
    @DisplayName("connexion : ne sauvegarde pas si le tenant est déjà aligné")
    void connecter_tenant_deja_aligne() {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID tenantId = UUID.randomUUID();
        DeveloperAccountEntity account =
                DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", tenantId, null, null, null, "FREE");

        when(authentifier.login("dev@x.co", "pw")).thenReturn(Mono.just(login(tenantId.toString())));
        when(repository.findByEmail("dev@x.co")).thenReturn(Mono.just(account));

        StepVerifier.create(service.connecter("dev@x.co", "pw"))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("connexion : extrait tid du JWT si selectedTenantId absent")
    void connecter_synchronise_tid_depuis_jwt() throws Exception {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID tenantId = UUID.randomUUID();
        var key = new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).generate();
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject("user-99")
                .claim("tid", tenantId.toString())
                .expirationTime(java.util.Date.from(java.time.Instant.now().plusSeconds(60)))
                .build();
        var jwt = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256), claims);
        jwt.sign(new com.nimbusds.jose.crypto.RSASSASigner(key));
        String token = jwt.serialize();

        DeveloperAccountEntity account = new DeveloperAccountEntity();
        account.setId(UUID.randomUUID());
        account.setEmail("dev@x.co");
        account.setPlan("FREE");
        account.setStatus("ACTIVE");

        when(authentifier.login("dev@x.co", "pw")).thenReturn(Mono.just(login(null, token)));
        when(repository.findByEmail("dev@x.co")).thenReturn(Mono.just(account));
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.connecter("dev@x.co", "pw"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<DeveloperAccountEntity> captor = ArgumentCaptor.forClass(DeveloperAccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKernelTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getKernelUserId()).isEqualTo("user-99");
    }
}
