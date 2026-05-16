package com.bcaas.core.actor.domain.model;

/**
 * Cycle de vie d'un acteur.
 * Analogie réseau : état d'une interface réseau (UP, DOWN, BLOCKED).
 */
public enum ActorStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}
