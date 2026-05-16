package com.bcaas.core.resource.domain.model;

/**
 * Type générique d'une ressource.
 * Chaque application métier définit ses propres types
 * via les metadata de la Resource.
 *
 * BCaaS ne connaît que STANDARD et TEMPLATE.
 * BuaaS spécialise : type=CAREER_SHEET, LEARNING_PATH
 * Transport spécialise : type=RIDE, VEHICLE
 */
public enum ResourceType {
    STANDARD,
    TEMPLATE,
    COMPOSITE
}
