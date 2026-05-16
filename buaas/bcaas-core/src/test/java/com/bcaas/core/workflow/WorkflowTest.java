package com.bcaas.core.workflow;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.event.WorkflowCompletedEvent;
import com.bcaas.core.workflow.domain.event.WorkflowFailedEvent;
import com.bcaas.core.workflow.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Workflow — moteur d'exécution")
class WorkflowTest {

    private final TenantId tenantId = TenantId.generate();
    private final ActorId actor = ActorId.generate();

    private List<WorkflowStep> threeSteps() {
        return List.of(
                new WorkflowStep("s1", "validation", 1),
                new WorkflowStep("s2", "traitement", 2),
                new WorkflowStep("s3", "notification", 3)
        );
    }

    @Test
    @DisplayName("Un workflow créé est en état CREATED")
    void shouldBeCreatedInitially() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.CREATED);
        assertThat(w.getSteps()).hasSize(3);
    }

    @Test
    @DisplayName("Démarrer un workflow passe à RUNNING et démarre la 1ère étape")
    void shouldStartAndRunFirstStep() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        w.start();

        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(w.getCurrentStep().getName()).isEqualTo("validation");
        assertThat(w.getCurrentStep().getStatus()).isEqualTo(WorkflowStepStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Compléter toutes les étapes termine le workflow")
    void shouldCompleteAfterAllSteps() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        w.start();
        w.completeCurrentStep(Map.of("result1", "ok"));
        w.completeCurrentStep(Map.of("result2", "ok"));
        w.completeCurrentStep(Map.of("result3", "ok"));

        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(w.getProgressPercent()).isEqualTo(100);
        assertThat(w.getDomainEvents().stream()
                .anyMatch(e -> e instanceof WorkflowCompletedEvent)).isTrue();
    }

    @Test
    @DisplayName("Une étape en échec passe le workflow en FAILED")
    void shouldFailOnStepError() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        w.start();
        w.failCurrentStep("Validation échouée");

        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        assertThat(w.getDomainEvents().stream()
                .anyMatch(e -> e instanceof WorkflowFailedEvent)).isTrue();
    }

    @Test
    @DisplayName("Un workflow FAILED peut être compensé (Saga)")
    void shouldCompensateFailedWorkflow() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        w.start();
        w.completeCurrentStep(Map.of());
        w.failCurrentStep("Erreur étape 2");

        w.compensate();

        assertThat(w.getStatus()).isEqualTo(WorkflowStatus.COMPENSATED);
        assertThat(w.getSteps().get(0).getStatus()).isEqualTo(WorkflowStepStatus.COMPENSATED);
    }

    @Test
    @DisplayName("Le contexte est propagé entre les étapes")
    void shouldPropagateContextBetweenSteps() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of("initialKey", "initialValue"));
        w.start();
        w.completeCurrentStep(Map.of("step1Output", "valeur1"));

        assertThat(w.getContextValue("step1Output")).isEqualTo("valeur1");
        assertThat(w.getContextValue("initialKey")).isEqualTo("initialValue");
    }

    @Test
    @DisplayName("Un workflow sans étapes est rejeté")
    void shouldRejectEmptySteps() {
        assertThatThrownBy(() ->
            Workflow.create(tenantId, "test", "corr", actor, List.of(), Map.of())
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("étape");
    }

    @Test
    @DisplayName("Progression en pourcentage correcte")
    void shouldCalculateProgressCorrectly() {
        Workflow w = Workflow.create(tenantId, "publication", "corr-1",
                actor, threeSteps(), Map.of());
        w.start();
        assertThat(w.getProgressPercent()).isEqualTo(0);

        w.completeCurrentStep(Map.of());
        assertThat(w.getProgressPercent()).isEqualTo(33);
    }
}
