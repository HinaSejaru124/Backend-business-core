package com.yowyob.businesscore.domain.operation;

import com.yowyob.businesscore.domain.shared.Declencheur;

import java.util.UUID;

/**
 * Brique 5 — Définition d'une Opération (l'en-tête du workflow déclaré).
 *
 * <p>Une Opération est le <b>verbe</b> du métier (vendre, réserver). Ce n'est pas du code : c'est une
 * donnée déclarée par le développeur sous une {@code VersionType}, composée d'une séquence ordonnée
 * d'{@link EtapeOperation} (étapes-types d'un catalogue fermé). Le moteur l'exécute en chaînant les
 * étapes via le port interne {@code ExecuteurDEtape} ; la compensation est gratuite (saga).
 *
 * <p>Record immuable, sans dépendance technique (domaine pur).
 */
public record DefinitionOperation(
        UUID id,
        UUID tenantId,            // développeur propriétaire (isolation RLS)
        UUID versionTypeId,       // FK vers la version de Type sous laquelle l'opération est déclarée
        String nom,               // ex. "vente", "reservation" — sert d'identifiant d'exécution
        String roleDeclencheur,   // rôle métier autorisé à déclencher (optionnel)
        Declencheur declencheurRegles, // point d'ancrage des règles évaluées par l'étape EVALUER_REGLES
        boolean differe           // false = immédiat (200), true = différé (202 + trace EN_COURS)
) {

    public DefinitionOperation {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (versionTypeId == null)
            throw new IllegalArgumentException("versionTypeId est obligatoire");
        if (nom == null || nom.isBlank())
            throw new IllegalArgumentException("nom est obligatoire");
        nom = nom.trim();
        roleDeclencheur = (roleDeclencheur == null || roleDeclencheur.isBlank()) ? null : roleDeclencheur.trim();
        // Défaut : l'ancrage générique avant l'opération si non précisé.
        declencheurRegles = declencheurRegles == null ? Declencheur.AVANT_OPERATION : declencheurRegles;
    }

    /** Fabrique : crée une nouvelle définition d'opération avec un identifiant généré. */
    public static DefinitionOperation creer(UUID tenantId, UUID versionTypeId, String nom,
                                            String roleDeclencheur, Declencheur declencheurRegles,
                                            boolean differe) {
        return new DefinitionOperation(
                UUID.randomUUID(), tenantId, versionTypeId, nom,
                roleDeclencheur, declencheurRegles, differe);
    }

    /** Deux opérations partagent-elles le même nom (comparaison insensible à la casse) ? */
    public boolean aPourNom(String autreNom) {
        return autreNom != null && nom.equalsIgnoreCase(autreNom.trim());
    }
}
