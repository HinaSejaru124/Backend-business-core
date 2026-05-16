package com.bcaas.core.tenant.domain.model;

/**
 * Cycle de vie d'un tenant dans le système BCaaS.
 * Analogie réseau : état d'une connexion (ESTABLISHED, CLOSED...).
 */
public enum TenantStatus {

    /**
     * En attente de validation — tenant créé mais pas encore actif.
     */
    PENDING,

    /**
     * Actif — le tenant peut utiliser le système normalement.
     */
    ACTIVE,

    /**
     * Suspendu — accès temporairement bloqué (impayé, violation...).
     */
    SUSPENDED,

    /**
     * Désactivé définitivement.
     */
    DEACTIVATED
}
