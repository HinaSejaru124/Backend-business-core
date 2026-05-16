package com.bcaas.core.audit.domain.model;

/**
 * Actions auditables génériques dans BCaaS.
 * Analogie réseau : type de paquet (SYN, ACK, DATA, FIN).
 */
public enum AuditAction {
    CREATE, READ, UPDATE, DELETE,
    ACTIVATE, SUSPEND, DEACTIVATE,
    PUBLISH, REJECT, ARCHIVE,
    LOGIN, LOGOUT, TOKEN_REFRESH,
    PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED,
    WORKFLOW_STARTED, WORKFLOW_COMPLETED, WORKFLOW_FAILED,
    EXPORT, IMPORT, BULK_ACTION
}
