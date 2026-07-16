package com.yowyob.businesscore.domain.businesstype;

import com.yowyob.businesscore.application.error.ProblemException;
import com.yowyob.businesscore.domain.shared.StatutType;

import java.util.UUID;

/**
 * Brique 1 — Type Métier.
 *
 * Gabarit réutilisable d'une catégorie d'activité (ex: "Pharmacie", "Restaurant").
 * Ce n'est PAS une classe Java figée — c'est une donnée déclarée par le développeur.
 *
 * Cycle de vie : BROUILLON → PUBLIE → ARCHIVE
 * Règle RG-03 : une fois PUBLIE, un TypeMetier ne peut plus être modifié directement.
 *              On crée une nouvelle VersionType à la place.
 */
public record TypeMetier(
        UUID id,
        UUID tenantId,          // le développeur propriétaire (isolation RLS)
        UUID businessDomainId,  // classement taxonomique kernel (optionnel)
        String code,            // identifiant unique ex: "PHARMACIE"
        String nom,             // nom lisible ex: "Pharmacie"
        StatutType statut       // BROUILLON / PUBLIE / ARCHIVE
) {

    // ─── Constructeur compact — validations à la création ─────────────────
    public TypeMetier {
        if (tenantId == null)
            throw new IllegalArgumentException("tenantId est obligatoire");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code est obligatoire");
        if (nom == null || nom.isBlank())
            throw new IllegalArgumentException("nom est obligatoire");
        // Normaliser le code : toujours majuscules sans espaces
        code = code.trim().toUpperCase();
        nom = nom.trim();
    }

    // ─── Fabrique statique (point d'entrée unique pour créer un type) ─────
    /**
     * Crée un nouveau TypeMetier en statut BROUILLON.
     * Le statut BROUILLON signifie : modifiable, pas encore utilisable
     * pour créer des entreprises.
     */
    public static TypeMetier creer(UUID tenantId, String code,
                                   String nom, UUID businessDomainId) {
        return new TypeMetier(
                UUID.randomUUID(),
                tenantId,
                businessDomainId,
                code,
                nom,
                StatutType.BROUILLON
        );
    }

    // ─── Transitions d'état ───────────────────────────────────────────────

    /**
     * Publie le TypeMetier : il devient utilisable pour créer des Entreprises.
     * Règle : seul un BROUILLON peut être publié.
     * Retourne une NOUVELLE instance (les records sont immuables en Java).
     */
    public TypeMetier publier() {
        if (this.statut != StatutType.BROUILLON) {
            throw ProblemException.conflict(
                "Seul un type en BROUILLON peut être publié. Statut actuel : " + this.statut
            ).violatedRule("STATUT_BROUILLON_REQUIS");
        }
        return new TypeMetier(id, tenantId, businessDomainId, code, nom, StatutType.PUBLIE);
    }

    /**
     * Archive le TypeMetier : plus aucune nouvelle Entreprise ne peut l'utiliser.
     * Les Entreprises existantes épinglées à une version restent fonctionnelles.
     * Règle : seul un type PUBLIE peut être archivé.
     */
    public TypeMetier archiver() {
        if (this.statut != StatutType.PUBLIE) {
            throw ProblemException.conflict(
                "Seul un type PUBLIE peut être archivé. Statut actuel : " + this.statut
            ).violatedRule("STATUT_PUBLIE_REQUIS");
        }
        return new TypeMetier(id, tenantId, businessDomainId, code, nom, StatutType.ARCHIVE);
    }

    // ─── Gardes métier ────────────────────────────────────────────────────

    /**
     * Vérifie que ce TypeMetier appartient bien au tenant donné.
     * Utilisé dans les use cases pour l'isolation : un dev ne touche
     * pas aux types d'un autre dev.
     */
    public void verifierAppartenance(UUID autretenantId) {
        if (!this.tenantId.equals(autretenantId)) {
            throw ProblemException.forbidden(
                "Ce Type Métier n'appartient pas à votre tenant."
            );
        }
    }

    /**
     * Vérifie que le type est dans un état qui permet de créer une nouvelle version.
     * Règle : on ne peut créer de version que sur un type PUBLIE.
     */
    public void verifierPeutVersionner() {
        if (this.statut != StatutType.PUBLIE) {
            throw ProblemException.conflict(
                "Impossible de créer une version : le type doit être PUBLIE. " +
                "Statut actuel : " + this.statut
            ).violatedRule("STATUT_PUBLIE_REQUIS");
        }
    }

    /**
     * Vérifie que le type est utilisable pour créer une Entreprise.
     * Règle : seul un type PUBLIE peut être instancié en Entreprise.
     */
    public void verifierUtilisable() {
        if (this.statut != StatutType.PUBLIE) {
            throw ProblemException.conflict(
                "Ce Type Métier n'est pas disponible (statut : " + this.statut + ")."
            ).violatedRule("STATUT_PUBLIE_REQUIS");
        }
    }
}
