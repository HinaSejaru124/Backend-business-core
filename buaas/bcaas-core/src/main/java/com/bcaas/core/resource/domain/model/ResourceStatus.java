package com.bcaas.core.resource.domain.model;

/**
 * Cycle de vie générique d'une ressource BCaaS.
 *
 * BuaaS : une fiche métier passe par DRAFT → REVIEW → PUBLISHED
 * Transport : un trajet passe par DRAFT → ACTIVE → COMPLETED
 * Santé : un dossier passe par DRAFT → ACTIVE → ARCHIVED
 */
public enum ResourceStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    ARCHIVED,
    REJECTED
}
