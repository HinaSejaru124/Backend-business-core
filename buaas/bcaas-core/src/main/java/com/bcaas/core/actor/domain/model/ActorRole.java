package com.bcaas.core.actor.domain.model;

/**
 * Rôle générique d'un acteur dans le système BCaaS.
 *
 * Analogie réseau : niveau de privilège d'accès (ACL).
 * Chaque application métier (BuaaS, transport, santé) spécialise
 * ces rôles génériques en rôles métier concrets.
 *
 * BuaaS :    OWNER=Freelance, MEMBER=Conseiller, CONSUMER=Étudiant
 * Transport : OWNER=Chauffeur, CONSUMER=Passager
 * Santé :    OWNER=Médecin,   MEMBER=Infirmier,  CONSUMER=Patient
 */
public enum ActorRole {

    /**
     * Propriétaire — contrôle total sur ses ressources.
     */
    OWNER,

    /**
     * Membre — peut créer et modifier des ressources dans le tenant.
     */
    MEMBER,

    /**
     * Consommateur — accès en lecture aux ressources publiées.
     */
    CONSUMER,

    /**
     * Administrateur — gestion du tenant (sans accès aux données métier).
     */
    ADMIN,

    /**
     * Invité — accès limité, non authentifié.
     */
    GUEST
}
