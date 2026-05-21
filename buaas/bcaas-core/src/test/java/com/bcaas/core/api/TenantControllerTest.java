package com.bcaas.core.api;

import com.bcaas.core.api.dto.request.CreateTenantRequest;
import com.bcaas.core.api.dto.response.TenantResponse;
import com.bcaas.core.api.rest.tenant.TenantController;
import com.bcaas.core.context.domain.BusinessContext;
import com.bcaas.core.context.domain.PolicyLevel;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.tenant.domain.model.*;
import com.bcaas.core.tenant.port.input.TenantUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantController — couche API")
class TenantControllerTest {

    @Mock
    private TenantUseCase tenantUseCase;

    @InjectMocks
    private TenantController tenantController;

    @Test
    @DisplayName("La réponse TenantResponse est correctement mappée depuis le domaine")
    void shouldMapTenantToResponse() {
        ActorId admin = ActorId.generate();
        Tenant tenant = Tenant.create(
                "Université de Yaoundé",
                "univ-yaounde",
                TenantPlan.STANDARD,
                TenantSettings.defaults(),
                admin
        );
        tenant.activate(admin);

        TenantResponse response = TenantResponse.from(tenant);

        assertThat(response.name()).isEqualTo("Université de Yaoundé");
        assertThat(response.slug()).isEqualTo("univ-yaounde");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.plan()).isEqualTo("STANDARD");
        assertThat(response.defaultLocale()).isEqualTo("fr");
        assertThat(response.defaultCurrency()).isEqualTo("XAF");
    }

    @Test
    @DisplayName("findById retourne un TenantResponse wrappé en ResponseEntity")
    void shouldReturnTenantById() {
        ActorId admin = ActorId.generate();
        Tenant tenant = Tenant.create(
                "Test Tenant", "test-tenant",
                TenantPlan.FREE, TenantSettings.defaults(), admin
        );

        when(tenantUseCase.findById(any(TenantId.class)))
                .thenReturn(Mono.just(tenant));

        StepVerifier.create(tenantController.findById(tenant.getId().toString()))
                .assertNext(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().name()).isEqualTo("Test Tenant");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("BusinessContext est correctement construit")
    void shouldBuildBusinessContext() {
        TenantId tenantId = TenantId.generate();
        ActorId actorId = ActorId.generate();

        BusinessContext context = BusinessContext.of(
                tenantId, actorId, "ADMIN", "fr", PolicyLevel.PREMIUM
        );

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.actorId()).isEqualTo(actorId);
        assertThat(context.roleScope()).isEqualTo("ADMIN");
        assertThat(context.locale()).isEqualTo("fr");
        assertThat(context.policyLevel()).isEqualTo(PolicyLevel.PREMIUM);
        assertThat(context.traceId()).isNotNull();
        assertThat(context.correlationId()).isNotNull();
    }
}
