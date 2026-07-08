package com.yowyob.businesscore.application.usecase.access;

import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.usecase.access.ApiKeyService.CleApiCreee;
import com.yowyob.businesscore.domain.port.out.AuthentifierUtilisateur;
import com.yowyob.businesscore.domain.port.out.SignUpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class RegistrationServiceTest {

    @Mock DeveloperAccountRepository repository;
    @Mock AuthentifierUtilisateur authentifier;
    @Mock ApiKeyService apiKeyService;

    @Test
    @DisplayName("inscrit : sign-up kernel, mémorise le tenant + email, émet une première clé")
    void inscrit_lie_tenant_et_emet_cle() {
        RegistrationService service = new RegistrationService(repository, authentifier, apiKeyService);
        UUID tenantId = UUID.randomUUID();
        UUID developerId = UUID.randomUUID();

        when(authentifier.signUp("dev@example.com", "MotDePasse1!", "Ada", "Lovelace"))
                .thenReturn(Mono.just(new SignUpResult("kernel-user-id", tenantId.toString(), "PENDING", "ok")));
        when(repository.save(any())).thenAnswer(inv -> {
            DeveloperAccountEntity e = inv.getArgument(0);
            e.setId(developerId);
            return Mono.just(e);
        });
        when(apiKeyService.creer(eq(developerId), eq("Default")))
                .thenReturn(Mono.just(new CleApiCreee(UUID.randomUUID(), "bck_abc", "secret-xyz", "Default")));

        StepVerifier.create(service.inscrire("Ada", "Lovelace", "dev@example.com", "MotDePasse1!", "FREE"))
                .assertNext(emise -> {
                    assertThat(emise.clientId()).isEqualTo("bck_abc");
                    assertThat(emise.apiKey()).isEqualTo("secret-xyz");
                    assertThat(emise.plan()).isEqualTo("FREE");
                })
                .verifyComplete();

        ArgumentCaptor<DeveloperAccountEntity> captor = ArgumentCaptor.forClass(DeveloperAccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKernelTenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().getEmail()).isEqualTo("dev@example.com");
    }

    @Test
    @DisplayName("aucune ClientApplication kernel n'est provisionnée à l'inscription")
    void inscrit_ne_provisionne_pas_de_client_kernel() {
        RegistrationService service = new RegistrationService(repository, authentifier, apiKeyService);
        when(authentifier.signUp(any(), any(), any(), any()))
                .thenReturn(Mono.just(new SignUpResult("id", null, "PENDING", "ok")));
        when(repository.save(any())).thenAnswer(inv -> {
            DeveloperAccountEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return Mono.just(e);
        });
        when(apiKeyService.creer(any(), any()))
                .thenReturn(Mono.just(new CleApiCreee(UUID.randomUUID(), "bck_x", "s", "Default")));

        StepVerifier.create(service.inscrire("A", "B", "a@b.co", "pw", null))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<DeveloperAccountEntity> captor = ArgumentCaptor.forClass(DeveloperAccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKernelClientId()).isNull();
        verify(apiKeyService, never()).renommer(any(), any(), any());
    }
}
