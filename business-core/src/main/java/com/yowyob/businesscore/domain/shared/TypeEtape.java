package com.yowyob.businesscore.domain.shared;

/**
 * Catalogue fermé d'étapes-types d'une opération (discipline anti-dérive).
 *
 * <p>Le développeur ordonne ces étapes pour déclarer un workflow ; il ne programme pas librement.
 * Chaque étape-type est exécutée par une stratégie derrière le port interne {@code ExecuteurDEtape}.
 * Contrat figé par le socle ; étendre le catalogue se fait par PR sur le socle.
 */
public enum TypeEtape {
    VERIFIER_STOCK,
    EVALUER_REGLES,
    ENREGISTRER_VENTE,
    ENCAISSER,
    EMETTRE_EVENEMENT,
    ATTACHER_DOCUMENT
}
