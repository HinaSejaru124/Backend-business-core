package com.yowyob.businesscore.domain.enterprise;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.shared.CycleVie;

import java.util.UUID;

/**
 * Brique 3 (liaison) — Entreprise : <b>instance</b> d'un Type Métier à une version épinglée.
 *
 * <p>Implémentation minimale fournie par la feature Opérations (Dev 5) car l'exécution d'une opération
 * a besoin de résoudre {@code businessId → versionTypeId / organizationId}. Cette brique appartient au
 * périmètre de Dev 3 ; cette version de base est destinée à être complétée/fusionnée avec son travail
 * (offres, acteurs, auto-provisionnement de l'organisation kernel).
 *
 * <p>Entité de liaison : porte une référence kernel ({@code organizationId}) + des métadonnées propres.
 * Record immuable du domaine.
 */
public record Entreprise(
        UUID id,
        UUID tenantId,
        UUID typeMetierId,
        UUID versionTypeId,     // version de Type sur laquelle l'entreprise est épinglée
        int numeroVersion,
        UUID organizationId,    // référence Organization kernel (optionnelle au stade minimal)
        String nom,
        CycleVie cycleVie
) {

    public Entreprise {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (versionTypeId == null)
            throw new IllegalArgumentException("versionTypeId est obligatoire");
        if (nom == null || nom.isBlank())
            throw new IllegalArgumentException("nom est obligatoire");
        nom = nom.trim();
        cycleVie = cycleVie == null ? CycleVie.ACTIVE : cycleVie;
    }

    /** Fabrique : crée une entreprise ACTIVE épinglée à une version de Type. */
    public static Entreprise creer(UUID tenantId, UUID typeMetierId, UUID versionTypeId,
                                   int numeroVersion, UUID organizationId, String nom) {
        return new Entreprise(UUID.randomUUID(), tenantId, typeMetierId, versionTypeId,
                numeroVersion, organizationId, nom, CycleVie.ACTIVE);
    }

    /** Transition de cycle de vie (ACTIVE / SUSPENDUE / FERMEE). */
    public Entreprise changerCycleVie(CycleVie nouveau) {
        if (nouveau == null) {
            throw ProblemException.badRequest("Le cycle de vie cible est obligatoire.");
        }
        return new Entreprise(id, tenantId, typeMetierId, versionTypeId, numeroVersion,
                organizationId, nom, nouveau);
    }

    public void verifierAppartenance(UUID autreTenantId) {
        if (!tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden("Cette entreprise n'appartient pas à votre tenant.");
        }
    }

    /** Une opération ne peut s'exécuter que sur une entreprise active. */
    public void verifierOperable() {
        if (cycleVie != CycleVie.ACTIVE) {
            throw ProblemException.conflict(
                    "L'entreprise n'est pas active (cycle de vie : " + cycleVie + ").")
                    .violatedRule("ENTREPRISE_NON_ACTIVE");
        }
    }
}
