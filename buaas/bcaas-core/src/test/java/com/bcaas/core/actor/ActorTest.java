package com.bcaas.core.actor;

import com.bcaas.core.actor.domain.event.ActorCreatedEvent;
import com.bcaas.core.actor.domain.event.ActorVerifiedEvent;
import com.bcaas.core.actor.domain.model.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Actor — règles métier")
class ActorTest {

    private final ActorId admin = ActorId.generate();
    private final TenantId tenantId = TenantId.generate();

    private ActorIdentity validIdentity() {
        return new ActorIdentity("jean@buaas.cm", "Jean", "Dupont", "+237600000000", "fr");
    }

    @Test
    @DisplayName("Un acteur créé est PENDING_VERIFICATION")
    void shouldBeInPendingState() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.OWNER, admin);

        assertThat(actor.getStatus()).isEqualTo(ActorStatus.PENDING_VERIFICATION);
        assertThat(actor.getDomainEvents()).hasSize(1);
        assertThat(actor.getDomainEvents().get(0)).isInstanceOf(ActorCreatedEvent.class);
    }

    @Test
    @DisplayName("Un acteur PENDING peut être vérifié")
    void shouldVerifyPendingActor() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.OWNER, admin);
        actor.clearDomainEvents();

        actor.verify(admin);

        assertThat(actor.getStatus()).isEqualTo(ActorStatus.ACTIVE);
        assertThat(actor.getDomainEvents().get(0)).isInstanceOf(ActorVerifiedEvent.class);
    }

    @Test
    @DisplayName("Un email invalide est rejeté")
    void shouldRejectInvalidEmail() {
        assertThatThrownBy(() ->
            new ActorIdentity("email-invalide", "Jean", "Dupont", null, "fr")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un acteur ACTIVE peut être suspendu")
    void shouldSuspendActiveActor() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.MEMBER, admin);
        actor.verify(admin);
        actor.clearDomainEvents();

        actor.suspend(admin, "Violation des CGU");

        assertThat(actor.getStatus()).isEqualTo(ActorStatus.SUSPENDED);
        assertThat(actor.isActive()).isFalse();
    }

    @Test
    @DisplayName("Un acteur peut changer de rôle")
    void shouldChangeRole() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.MEMBER, admin);
        actor.verify(admin);

        actor.changeRole(ActorRole.ADMIN, admin);

        assertThat(actor.getRole()).isEqualTo(ActorRole.ADMIN);
    }

    @Test
    @DisplayName("Changer vers le même rôle est rejeté")
    void shouldRejectSameRoleChange() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.MEMBER, admin);

        assertThatThrownBy(() -> actor.changeRole(ActorRole.MEMBER, admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un acteur peut enrichir son profil")
    void shouldUpdateProfile() {
        Actor actor = Actor.create(tenantId, validIdentity(), ActorRole.OWNER, admin);
        actor.verify(admin);

        ActorProfile profile = ActorProfile.empty()
                .withMetadata("specialization", "Ingénierie informatique")
                .withMetadata("yearsOfExperience", "5");

        actor.updateProfile(profile, admin);

        assertThat(actor.getProfile().getMetadata("specialization"))
                .isEqualTo("Ingénierie informatique");
    }
}
