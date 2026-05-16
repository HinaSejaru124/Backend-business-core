package com.bcaas.core.workflow.domain.model;

import com.bcaas.core.shared.domain.ActorId;
import com.bcaas.core.shared.domain.TenantId;
import com.bcaas.core.workflow.domain.event.*;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root du domaine Workflow.
 *
 * Un Workflow est une séquence d'étapes ordonnées représentant
 * un processus métier générique. BCaaS fournit le moteur d'exécution,
 * les applications définissent les étapes.
 *
 * Pattern : Saga — chaque étape peut être compensée en cas d'échec.
 * Analogie réseau : protocole de handshake multi-étapes avec rollback.
 *
 * Exemples d'usage :
 * BuaaS    : workflow de publication d'une fiche métier
 * Transport : workflow de réservation d'un trajet
 * Santé    : workflow d'admission d'un patient
 */
public class Workflow {

    private final WorkflowId id;
    private final TenantId tenantId;
    private final String workflowType;
    private final String correlationId;
    private final ActorId initiatedBy;
    private WorkflowStatus status;
    private final List<WorkflowStep> steps;
    private int currentStepIndex;
    private Instant startedAt;
    private Instant completedAt;
    private final Map<String, String> context;
    private final List<WorkflowDomainEvent> domainEvents = new ArrayList<>();

    private Workflow(WorkflowId id, TenantId tenantId, String workflowType,
                     String correlationId, ActorId initiatedBy,
                     List<WorkflowStep> steps, Map<String, String> context) {
        this.id = id;
        this.tenantId = tenantId;
        this.workflowType = workflowType;
        this.correlationId = correlationId;
        this.initiatedBy = initiatedBy;
        this.status = WorkflowStatus.CREATED;
        this.steps = new ArrayList<>(steps);
        this.currentStepIndex = 0;
        this.context = new HashMap<>(context);
    }

    public static Workflow create(TenantId tenantId, String workflowType,
                                   String correlationId, ActorId initiatedBy,
                                   List<WorkflowStep> steps,
                                   Map<String, String> context) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Un workflow doit avoir au moins une étape");
        }
        if (workflowType == null || workflowType.isBlank()) {
            throw new IllegalArgumentException("Le type de workflow est obligatoire");
        }

        WorkflowId id = WorkflowId.generate();
        Workflow workflow = new Workflow(
                id, tenantId, workflowType, correlationId, initiatedBy, steps, context
        );
        workflow.domainEvents.add(new WorkflowCreatedEvent(id, tenantId, workflowType, correlationId));
        return workflow;
    }

    // ================================================================
    // Commandes métier
    // ================================================================

    public void start() {
        if (this.status != WorkflowStatus.CREATED) {
            throw new IllegalStateException("Le workflow ne peut démarrer que depuis CREATED");
        }
        this.status = WorkflowStatus.RUNNING;
        this.startedAt = Instant.now();
        this.domainEvents.add(new WorkflowStartedEvent(this.id, this.tenantId));

        // Démarrer la première étape
        if (!steps.isEmpty()) {
            steps.get(0).start(context);
        }
    }

    public void completeCurrentStep(Map<String, String> outputData) {
        assertRunning();
        WorkflowStep currentStep = getCurrentStep();
        currentStep.complete(outputData);

        // Mettre à jour le contexte avec les outputs de l'étape
        context.putAll(outputData);

        // Passer à l'étape suivante ou terminer
        if (hasNextStep()) {
            currentStepIndex++;
            steps.get(currentStepIndex).start(context);
        } else {
            complete();
        }
    }

    public void failCurrentStep(String errorMessage) {
        assertRunning();
        getCurrentStep().fail(errorMessage);
        this.status = WorkflowStatus.FAILED;
        this.completedAt = Instant.now();
        this.domainEvents.add(new WorkflowFailedEvent(
                this.id, this.tenantId, getCurrentStep().getName(), errorMessage));
    }

    public void compensate() {
        if (this.status != WorkflowStatus.FAILED) {
            throw new IllegalStateException("Seul un workflow FAILED peut être compensé");
        }
        this.status = WorkflowStatus.COMPENSATING;

        // Compenser toutes les étapes complétées en ordre inverse
        for (int i = currentStepIndex; i >= 0; i--) {
            WorkflowStep step = steps.get(i);
            if (step.isCompleted()) {
                step.compensate();
            }
        }

        this.status = WorkflowStatus.COMPENSATED;
        this.completedAt = Instant.now();
        this.domainEvents.add(new WorkflowCompensatedEvent(this.id, this.tenantId));
    }

    public void skipCurrentStep() {
        assertRunning();
        getCurrentStep().skip();

        if (hasNextStep()) {
            currentStepIndex++;
            steps.get(currentStepIndex).start(context);
        } else {
            complete();
        }
    }

    // ================================================================
    // Règles métier (queries)
    // ================================================================

    public boolean isRunning() { return status == WorkflowStatus.RUNNING; }
    public boolean isCompleted() { return status == WorkflowStatus.COMPLETED; }
    public boolean isFailed() { return status == WorkflowStatus.FAILED; }

    public WorkflowStep getCurrentStep() {
        if (steps.isEmpty()) throw new IllegalStateException("Aucune étape dans ce workflow");
        return steps.get(currentStepIndex);
    }

    public Optional<WorkflowStep> getStepByName(String name) {
        return steps.stream().filter(s -> s.getName().equals(name)).findFirst();
    }

    public int getProgressPercent() {
        if (steps.isEmpty()) return 0;
        long completed = steps.stream().filter(s ->
                s.isCompleted() || s.getStatus() == WorkflowStepStatus.SKIPPED).count();
        return (int) (completed * 100 / steps.size());
    }

    public String getContextValue(String key) {
        return context.getOrDefault(key, null);
    }

    // ================================================================
    // Privé
    // ================================================================

    private void assertRunning() {
        if (this.status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException("Le workflow doit être RUNNING");
        }
    }

    private boolean hasNextStep() {
        return currentStepIndex < steps.size() - 1;
    }

    private void complete() {
        this.status = WorkflowStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.domainEvents.add(new WorkflowCompletedEvent(this.id, this.tenantId, workflowType));
    }

    // ================================================================
    // Événements & Getters
    // ================================================================

    public List<WorkflowDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    public void clearDomainEvents() { domainEvents.clear(); }

    public WorkflowId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getWorkflowType() { return workflowType; }
    public String getCorrelationId() { return correlationId; }
    public ActorId getInitiatedBy() { return initiatedBy; }
    public WorkflowStatus getStatus() { return status; }
    public List<WorkflowStep> getSteps() { return Collections.unmodifiableList(steps); }
    public int getCurrentStepIndex() { return currentStepIndex; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Map<String, String> getContext() { return Map.copyOf(context); }
}
