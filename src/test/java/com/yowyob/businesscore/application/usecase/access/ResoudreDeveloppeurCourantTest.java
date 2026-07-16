package com.yowyob.businesscore.application.usecase.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yowyob.businesscore.adapter.in.security.JwtAuthenticationToken;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountEntity;
import com.yowyob.businesscore.adapter.out.persistence.developer.DeveloperAccountRepository;
import com.yowyob.businesscore.application.context.BusinessContext;
import com.yowyob.businesscore.application.context.BusinessContextHolder;
import com.yowyob.businesscore.application.error.ProblemException;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ResoudreDeveloppeurCourantTest {

    private DeveloperAccountRepository repository;
    private ResoudreDeveloppeurCourant resoudre;

    @BeforeEach
    void setUp() {
        repository = mock(DeveloperAccountRepository.class);
        resoudre = new ResoudreDeveloppeurCourant(repository);
    }

    @Test
    @DisplayName("contexte absent (drain WebFlux) → Mono.empty() sans erreur")
    void contexte_absent_retourne_vide() {
        StepVerifier.create(resoudre.id())
                .verifyComplete();
    }

    @Test
    @DisplayName("tenant connu → retourne l'id développeur")
    void tenant_connu() {
        UUID tenantId = UUID.randomUUID();
        UUID devId = UUID.randomUUID();
        BusinessContext ctx = new BusinessContext(tenantId, null, Set.of(), null, "trace", Locale.FRENCH);
        DeveloperAccountEntity account = new DeveloperAccountEntity();
        account.setId(devId);
        when(repository.findByKernelTenantId(tenantId)).thenReturn(Mono.just(account));

        StepVerifier.create(resoudre.id().contextWrite(c -> BusinessContextHolder.withContext(c, ctx)))
                .assertNext(id -> assertThat(id).isEqualTo(devId))
                .verifyComplete();
    }

    @Test
    @DisplayName("tenant inconnu mais JWT SecurityContext → résout par sub")
    void repli_security_context() {
        UUID tenantId = UUID.randomUUID();
        UUID devId = UUID.randomUUID();
        String kernelUserId = UUID.randomUUID().toString();
        BusinessContext ctx = new BusinessContext(tenantId, null, Set.of(), null, "trace", Locale.FRENCH);

        DeveloperAccountEntity account = new DeveloperAccountEntity();
        account.setId(devId);
        account.setKernelUserId(kernelUserId);

        when(repository.findByKernelTenantId(tenantId)).thenReturn(Mono.empty());
        when(repository.findByKernelUserId(kernelUserId)).thenReturn(Mono.just(account));
        when(repository.save(account)).thenReturn(Mono.just(account));

        String jwt = "header."
                + java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(("{\"sub\":\"" + kernelUserId + "\"}").getBytes())
                + ".sig";
        JwtAuthenticationToken auth = JwtAuthenticationToken.authenticated(jwt, ctx);
        SecurityContext securityContext = new SecurityContextImpl(auth);

        StepVerifier.create(resoudre.id()
                        .contextWrite(c -> BusinessContextHolder.withContext(c, ctx))
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext))))
                .assertNext(id -> assertThat(id).isEqualTo(devId))
                .verifyComplete();
    }

    @Test
    @DisplayName("développeur introuvable → 404")
    void developpeur_introuvable() {
        UUID tenantId = UUID.randomUUID();
        BusinessContext ctx = new BusinessContext(tenantId, null, Set.of(), null, "trace", Locale.FRENCH);
        when(repository.findByKernelTenantId(tenantId)).thenReturn(Mono.empty());

        StepVerifier.create(resoudre.id().contextWrite(c -> BusinessContextHolder.withContext(c, ctx)))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ProblemException.class);
                    assertThat(((ProblemException) err).getStatus()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
                })
                .verify();
    }
}
