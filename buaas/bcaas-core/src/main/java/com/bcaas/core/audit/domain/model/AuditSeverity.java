package com.bcaas.core.audit.domain.model;

/**
 * Niveau de criticité d'un événement d'audit.
 * Analogie réseau : priorité d'un paquet (DSCP/QoS).
 */
public enum AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}
