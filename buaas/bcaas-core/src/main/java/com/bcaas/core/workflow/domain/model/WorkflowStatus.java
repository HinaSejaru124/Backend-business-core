package com.bcaas.core.workflow.domain.model;

/**
 * Statut global d'un workflow.
 * Analogie réseau : état d'une session TCP (SYN, ESTABLISHED, FIN).
 */
public enum WorkflowStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
