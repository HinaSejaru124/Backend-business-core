package com.bcaas.core.workflow.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Étape d'un workflow générique.
 *
 * Chaque étape a un nom, un ordre d'exécution, un statut
 * et des données contextuelles (input/output).
 *
 * Analogie réseau : segment d'un pipeline de traitement.
 * Le workflow orchestre les étapes comme un routeur
 * orchestre les paquets à travers les nœuds du réseau.
 */
public class WorkflowStep {

    private final String stepId;
    private final String name;
    private final int order;
    private WorkflowStepStatus status;
    private Map<String, String> inputData;
    private Map<String, String> outputData;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    public WorkflowStep(String stepId, String name, int order) {
        this.stepId = stepId;
        this.name = name;
        this.order = order;
        this.status = WorkflowStepStatus.PENDING;
        this.inputData = new HashMap<>();
        this.outputData = new HashMap<>();
    }

    public void start(Map<String, String> inputData) {
        if (this.status != WorkflowStepStatus.PENDING) {
            throw new IllegalStateException(
                "L'étape " + name + " ne peut démarrer que depuis PENDING");
        }
        this.status = WorkflowStepStatus.IN_PROGRESS;
        this.inputData = new HashMap<>(inputData);
        this.startedAt = Instant.now();
    }

    public void complete(Map<String, String> outputData) {
        if (this.status != WorkflowStepStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "L'étape " + name + " doit être IN_PROGRESS pour être complétée");
        }
        this.status = WorkflowStepStatus.COMPLETED;
        this.outputData = new HashMap<>(outputData);
        this.completedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = WorkflowStepStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public void skip() {
        this.status = WorkflowStepStatus.SKIPPED;
        this.completedAt = Instant.now();
    }

    public void compensate() {
        this.status = WorkflowStepStatus.COMPENSATED;
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() { return status == WorkflowStepStatus.COMPLETED; }
    public boolean isFailed() { return status == WorkflowStepStatus.FAILED; }
    public boolean isPending() { return status == WorkflowStepStatus.PENDING; }

    public String getStepId() { return stepId; }
    public String getName() { return name; }
    public int getOrder() { return order; }
    public WorkflowStepStatus getStatus() { return status; }
    public Map<String, String> getInputData() { return Map.copyOf(inputData); }
    public Map<String, String> getOutputData() { return Map.copyOf(outputData); }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
