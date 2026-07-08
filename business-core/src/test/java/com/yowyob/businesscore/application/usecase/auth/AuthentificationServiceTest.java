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
        return new ResultatLogin("jwt", 900, List.of("organizations:write"), List.of(), tenantId, "actor-1");
    }

    @Test
    @DisplayName("connexion : lie le tenant kernel au compte si absent")
    void connecter_lie_tenant_absent() {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID tenantId = UUID.randomUUID();
        DeveloperAccountEntity account =
                DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", null, null, null, "FREE");

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
    @DisplayName("connexion : ne réécrit pas le tenant déjà lié")
    void connecter_tenant_deja_lie() {
        AuthentificationService service = new AuthentificationService(authentifier, repository);
        UUID existant = UUID.randomUUID();
        DeveloperAccountEntity account =
                DeveloperAccountEntity.nouveau(UUID.randomUUID(), "dev@x.co", existant, null, null, "FREE");

        when(authentifier.login("dev@x.co", "pw")).thenReturn(Mono.just(login(UUID.randomUUID().toString())));
        when(repository.findByEmail("dev@x.co")).thenReturn(Mono.just(account));

        StepVerifier.create(service.connecter("dev@x.co", "pw"))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository, never()).save(any());
    }
}
