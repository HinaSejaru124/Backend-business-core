package com.bcaas.core.resource;

import com.bcaas.core.resource.domain.event.ResourcePublishedEvent;
import com.bcaas.core.resource.domain.model.*;
import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Resource — règles métier")
class ResourceTest {

    private final ActorId owner = ActorId.generate();
    private final ActorId admin = ActorId.generate();
    private final TenantId tenantId = TenantId.generate();

    private ResourceContent validContent() {
        return ResourceContent.of("Ingénieur Logiciel", "Description du métier", "fr");
    }

    @Test
    @DisplayName("Une resource créée est en DRAFT")
    void shouldBeInDraftAfterCreation() {
        Resource resource = Resource.create(tenantId, owner, validContent(), ResourceType.STANDARD);

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.DRAFT);
        assertThat(resource.isPublic()).isFalse();
    }

    @Test
    @DisplayName("Cycle de vie complet DRAFT → PUBLISHED")
    void shouldFollowPublicationLifecycle() {
        Resource resource = Resource.create(tenantId, owner, validContent(), ResourceType.STANDARD);

        resource.submitForReview(owner);
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.PENDING_REVIEW);

        resource.publish(admin);
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.PUBLISHED);
        assertThat(resource.isPublic()).isTrue();
        assertThat(resource.getDomainEvents().stream()
                .anyMatch(e -> e instanceof ResourcePublishedEvent)).isTrue();
    }

    @Test
    @DisplayName("REJECTED → DRAFT → re-soumission")
    void shouldAllowResubmissionAfterRejection() {
        Resource resource = Resource.create(tenantId, owner, validContent(), ResourceType.STANDARD);
        resource.submitForReview(owner);
        resource.reject(admin, "Contenu insuffisant");

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.REJECTED);

        resource.submitForReview(owner);
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("PUBLISHED → ARCHIVED est irréversible")
    void shouldArchivePublishedResource() {
        Resource resource = Resource.create(tenantId, owner, validContent(), ResourceType.STANDARD);
        resource.submitForReview(owner);
        resource.publish(admin);

        resource.archive(admin);

        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.ARCHIVED);
        assertThatThrownBy(() -> resource.archive(admin))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Un non-owner ne peut pas modifier la resource")
    void shouldRejectModificationByNonOwner() {
        Resource resource = Resource.create(tenantId, owner, validContent(), ResourceType.STANDARD);
        ActorId stranger = ActorId.generate();

        assertThatThrownBy(() -> resource.updateContent(validContent(), stranger))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("Un titre vide est rejeté")
    void shouldRejectEmptyTitle() {
        assertThatThrownBy(() ->
            ResourceContent.of("", "description", "fr")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Le contenu est extensible via les fields")
    void shouldSupportExtensibleContent() {
        ResourceContent content = ResourceContent.of("Médecin", "Description", "fr")
                .withField("salary_range", "300000-800000 XAF")
                .withField("education_level", "Doctorat");

        assertThat(content.getField("salary_range")).isEqualTo("300000-800000 XAF");
        assertThat(content.getField("education_level")).isEqualTo("Doctorat");
    }
}
