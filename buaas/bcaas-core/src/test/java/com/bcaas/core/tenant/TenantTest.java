package com.bcaas.core.tenant;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.tenant.domain.event.TenantActivatedEvent;
import com.bcaas.core.tenant.domain.event.TenantCreatedEvent;
import com.bcaas.core.tenant.domain.model.Tenant;
import com.bcaas.core.tenant.domain.model.TenantPlan;
import com.bcaas.core.tenant.domain.model.TenantSettings;
import com.bcaas.core.tenant.domain.model.TenantStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Tenant — règles métier")
class TenantTest {

    private final ActorId admin = ActorId.generate();

    @Test
    @DisplayName("Un tenant créé est en état PENDING")
    void shouldBeInPendingStateAfterCreation() {
        Tenant tenant = Tenant.create(
                "Université de Yaoundé",
                "univ-yaounde",
                TenantPlan.STANDARD,
                TenantSettings.defaults(),
                admin
        );

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.PENDING);
        assertThat(tenant.getDomainEvents()).hasSize(1);
        assertThat(tenant.getDomainEvents().get(0)).isInstanceOf(TenantCreatedEvent.class);
    }

    @Test
    @DisplayName("Un tenant PENDING peut être activé")
    void shouldActivateFromPending() {
        Tenant tenant = Tenant.create(
                "Université de Yaoundé",
                "univ-yaounde",
                TenantPlan.STANDARD,
                TenantSettings.defaults(),
                admin
        );
        tenant.clearDomainEvents();

        tenant.activate(admin);

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getDomainEvents()).hasSize(1);
        assertThat(tenant.getDomainEvents().get(0)).isInstanceOf(TenantActivatedEvent.class);
    }

    @Test
    @DisplayName("Un tenant DEACTIVATED ne peut pas être réactivé")
    void shouldNotReactivateDeactivatedTenant() {
        Tenant tenant = Tenant.create(
                "Test",
                "test-tenant",
                TenantPlan.FREE,
                TenantSettings.defaults(),
                admin
        );
        tenant.activate(admin);
        tenant.deactivate(admin);

        assertThatThrownBy(() -> tenant.activate(admin))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    @DisplayName("Un slug invalide est rejeté à la création")
    void shouldRejectInvalidSlug() {
        assertThatThrownBy(() -> Tenant.create(
                "Test",
                "Slug Invalide!",
                TenantPlan.FREE,
                TenantSettings.defaults(),
                admin
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("slug");
    }

    @Test
    @DisplayName("Un tenant FREE ne peut pas dépasser sa limite d'acteurs")
    void shouldEnforceActorLimitForFreePlan() {
        Tenant tenant = Tenant.create(
                "Test",
                "test-free",
                TenantPlan.FREE,
                TenantSettings.defaults(),
                admin
        );
        tenant.activate(admin);

        assertThat(tenant.canCreateActor(9)).isTrue();
        assertThat(tenant.canCreateActor(10)).isFalse();
    }

    @Test
    @DisplayName("Un upgrade de plan vers un plan inférieur est rejeté")
    void shouldRejectDowngrade() {
        Tenant tenant = Tenant.create(
                "Test",
                "test-upgrade",
                TenantPlan.PREMIUM,
                TenantSettings.defaults(),
                admin
        );
        tenant.activate(admin);

        assertThatThrownBy(() -> tenant.upgradePlan(TenantPlan.STANDARD, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supérieur");
    }
}
