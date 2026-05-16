package com.bcaas.core.context.domain;

/**
 * Niveau de politique / QoS appliqué à une requête.
 * Analogie réseau : Quality of Service (QoS).
 *
 * Détermine la priorité de traitement, les garanties de livraison
 * et les limites de rate limiting appliquées à la requête.
 */
public enum PolicyLevel {

    /**
     * Accès gratuit — limites strictes, pas de garantie de SLA.
     */
    FREE,

    /**
     * Accès standard — limites normales, SLA best-effort.
     */
    STANDARD,

    /**
     * Accès premium — priorité élevée, SLA garanti.
     */
    PREMIUM,

    /**
     * Accès entreprise — priorité maximale, SLA contractuel.
     */
    ENTERPRISE,

    /**
     * Accès système — réservé aux appels internes entre services.
     */
    SYSTEM
}
