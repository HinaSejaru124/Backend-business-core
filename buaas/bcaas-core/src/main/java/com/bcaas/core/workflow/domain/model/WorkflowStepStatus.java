package com.bcaas.core.workflow.domain.model;

/**
 * Statut d'une étape dans un workflow.
 * Analogie réseau : état d'un paquet dans le pipeline de traitement.
 */
public enum WorkflowStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    FAILED,
    COMPENSATED
}
