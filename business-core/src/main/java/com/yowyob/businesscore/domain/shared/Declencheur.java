package com.yowyob.businesscore.domain.shared;

/**
 * Points d'ancrage des règles dans les étapes d'opération (vocabulaire des déclencheurs).
 *
 * <p>Contrat figé par le socle : c'est l'unique point de contact entre la brique Règles (Dev 4),
 * qui déclare une règle sur un déclencheur, et la brique Opérations (Dev 5), qui évalue les règles
 * à ce point d'ancrage. Étendre le catalogue se fait par PR sur le socle.
 */
public enum Declencheur {
    AVANT_OPERATION,
    APRES_OPERATION,
    AVANT_VENTE,
    APRES_VENTE,
    AVANT_ENCAISSEMENT,
    APRES_ENCAISSEMENT,
    AVANT_RESERVATION,
    AVANT_LIVRAISON
}
