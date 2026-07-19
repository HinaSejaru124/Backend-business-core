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
        UUID businessActorId,   // business actor propriétaire kernel (créé à l'onboarding), mémorisé
        UUID agencyId,          // agence principale kernel, résolue/mémorisée (cf. ResolveurContexteKernel)
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
                numeroVersion, organizationId, null, null, nom, CycleVie.ACTIVE);
    }

    /** Transition de cycle de vie (ACTIVE / SUSPENDUE / FERMEE). */
    public Entreprise changerCycleVie(CycleVie nouveau) {
        if (nouveau == null) {
            throw ProblemException.badRequest("Le cycle de vie cible est obligatoire.");
        }
        return new Entreprise(id, tenantId, typeMetierId, versionTypeId, numeroVersion,
                organizationId, businessActorId, agencyId, nom, nouveau);
    }

    /** Renomme l'application (métadonnée locale ; pas de sync kernel dans cette version). */
    public Entreprise renommer(String nouveauNom) {
        if (nouveauNom == null || nouveauNom.isBlank()) {
            throw ProblemException.badRequest("Le nom de l'application est obligatoire.");
        }
        return new Entreprise(id, tenantId, typeMetierId, versionTypeId, numeroVersion,
                organizationId, businessActorId, agencyId, nouveauNom.trim(), cycleVie);
    }

    /** Mémorise les références kernel produites à l'auto-provisionnement (onboarding + organisation). */
    public Entreprise avecReferencesKernel(UUID businessActorId, UUID organizationId, UUID agencyId) {
        return new Entreprise(id, tenantId, typeMetierId, versionTypeId, numeroVersion,
                organizationId, businessActorId, agencyId, nom, cycleVie);
    }

    /** Mémorise l'agence principale résolue auprès du kernel. */
    public Entreprise avecAgence(UUID agencyId) {
        return new Entreprise(id, tenantId, typeMetierId, versionTypeId, numeroVersion,
                organizationId, businessActorId, agencyId, nom, cycleVie);
    }

    public void verifierAppartenance(UUID autreTenantId) {
        if (!tenantId.equals(autreTenantId)) {
            throw ProblemException.forbidden("Cette application n'appartient pas à votre tenant.");
        }
    }

    /** Une opération ne peut s'exécuter que sur une application active. */
    public void verifierOperable() {
        if (cycleVie != CycleVie.ACTIVE) {
            throw ProblemException.conflict(
                    "L'application n'est pas active (cycle de vie : " + cycleVie + ").")
                    .violatedRule("ENTREPRISE_NON_ACTIVE");
        }
    }
}
